// PortraitFetcher: downloads each saint portrait listed in saint_portrait_sources.md and saves it
// as a 256 x 256 JPEG, scaled so the longer side is 256 px and padded evenly with black.
//
// Usage (from anywhere inside the repo):
//   dotnet run --project tools/PortraitFetcher                 download every missing portrait
//   dotnet run --project tools/PortraitFetcher -- --force      re-download and overwrite all
//   dotnet run --project tools/PortraitFetcher -- --only anne.jpg
//   dotnet run --project tools/PortraitFetcher -- --convert <input image> <output.jpg>

using System.Net;
using System.Text.Json;
using System.Text.RegularExpressions;
using SkiaSharp;

const int Size = 256;
const int JpegQuality = 92;
// Ask Commons for a thumbnail this wide instead of the original, which can be hundreds of megapixels.
const int ThumbWidth = 1024;
const string UserAgent = "WayOfLifePortraitFetcher/1.0 (https://github.com/johnamarante/the-way-of-life)";

if (args.Length == 3 && args[0] == "--convert")
{
    File.WriteAllBytes(args[2], Letterbox(File.ReadAllBytes(args[1])));
    Console.WriteLine($"Wrote {args[2]}");
    return 0;
}

var force = args.Contains("--force");
var only = ArgValue("--only");
var root = FindRepoRoot();
var sourcesPath = ArgValue("--sources") ?? Path.Combine(root, "saint_portrait_sources.md");
var outDir = ArgValue("--out") ?? Path.Combine(root, "portraits");
Directory.CreateDirectory(outDir);

var entries = ParseSources(sourcesPath)
    .Where(e => only is null || e.FileName.Equals(only, StringComparison.OrdinalIgnoreCase))
    .ToList();
var todo = entries.Where(e => force || !File.Exists(Path.Combine(outDir, e.FileName))).ToList();
Console.WriteLine($"{entries.Count} portraits listed, {entries.Count - todo.Count} already saved, {todo.Count} to download.");
if (todo.Count == 0) return 0;

using var http = new HttpClient { Timeout = TimeSpan.FromMinutes(2) };
http.DefaultRequestHeaders.UserAgent.ParseAdd(UserAgent);

var thumbUrls = await GetThumbUrls(http, todo.Select(e => e.CommonsTitle).Distinct().ToList());
var failures = new List<string>();
var done = 0;
foreach (var entry in todo)
{
    try
    {
        if (!thumbUrls.TryGetValue(entry.CommonsTitle, out var url))
            throw new InvalidOperationException("Commons did not return an image URL (file renamed or deleted?)");
        var bytes = await GetWithRetry(http, url);
        var target = Path.Combine(outDir, entry.FileName);
        var temp = target + ".part";
        File.WriteAllBytes(temp, Letterbox(bytes));
        File.Move(temp, target, overwrite: true);
        Console.WriteLine($"[{++done}/{todo.Count}] {entry.FileName}");
    }
    catch (Exception ex)
    {
        failures.Add($"{entry.FileName}: {ex.Message}");
        Console.WriteLine($"[{++done}/{todo.Count}] FAILED {entry.FileName}: {ex.Message}");
    }
    // Be polite to Wikimedia's servers.
    await Task.Delay(TimeSpan.FromSeconds(1));
}

Console.WriteLine($"Done: {todo.Count - failures.Count} saved, {failures.Count} failed.");
failures.ForEach(f => Console.WriteLine("  " + f));
return failures.Count == 0 ? 0 : 1;

// Scales the image so its longer side is exactly Size (down or up) and centers it on a black square.
static byte[] Letterbox(byte[] imageBytes)
{
    using var source = SKBitmap.Decode(imageBytes)
        ?? throw new InvalidDataException("Not a readable image");
    using var image = SKImage.FromBitmap(source);
    var scale = (float)Size / Math.Max(source.Width, source.Height);
    var width = Math.Max(1, (int)Math.Round(source.Width * scale));
    var height = Math.Max(1, (int)Math.Round(source.Height * scale));
    var left = (Size - width) / 2;
    var top = (Size - height) / 2;
    var sampling = scale < 1
        ? new SKSamplingOptions(SKFilterMode.Linear, SKMipmapMode.Linear)
        : new SKSamplingOptions(SKCubicResampler.Mitchell);

    using var surface = SKSurface.Create(new SKImageInfo(Size, Size, SKColorType.Rgba8888, SKAlphaType.Premul));
    var canvas = surface.Canvas;
    canvas.Clear(SKColors.Black);
    canvas.DrawImage(image, SKRect.Create(left, top, width, height), sampling);
    using var result = surface.Snapshot();
    using var data = result.Encode(SKEncodedImageFormat.Jpeg, JpegQuality);
    return data.ToArray();
}

static List<(string FileName, string CommonsTitle)> ParseSources(string path)
{
    // Table rows look like: | 2 | St. Anne | `anne.jpg` | [label](https://commons.wikimedia.org/wiki/File:...) | ...
    var row = new Regex(@"^\|\s*\d+\s*\|[^|]*\|\s*`([^`]+)`\s*\|\s*\[[^\]]*\]\(https://commons\.wikimedia\.org/wiki/File:([^)\s]+)\)");
    var result = new List<(string, string)>();
    foreach (var line in File.ReadLines(path))
    {
        var match = row.Match(line);
        if (match.Success)
            result.Add((match.Groups[1].Value, Uri.UnescapeDataString(match.Groups[2].Value).Replace('_', ' ')));
    }
    if (result.Count == 0) throw new InvalidDataException($"No portrait rows found in {path}");
    return result;
}

static async Task<Dictionary<string, string>> GetThumbUrls(HttpClient http, List<string> titles)
{
    var urls = new Dictionary<string, string>();
    foreach (var batch in titles.Chunk(50))
    {
        var query = string.Join("|", batch.Select(t => "File:" + t));
        var url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2&prop=imageinfo"
            + $"&iiprop=url&iiurlwidth={ThumbWidth}&titles={Uri.EscapeDataString(query)}";
        using var doc = JsonDocument.Parse(await GetWithRetry(http, url));
        var q = doc.RootElement.GetProperty("query");
        // Commons may normalize titles (for example Unicode forms); map them back to what we asked for.
        var asked = new Dictionary<string, string>();
        if (q.TryGetProperty("normalized", out var normalized))
            foreach (var n in normalized.EnumerateArray())
                asked[n.GetProperty("to").GetString()!] = n.GetProperty("from").GetString()!;
        foreach (var page in q.GetProperty("pages").EnumerateArray())
        {
            if (!page.TryGetProperty("imageinfo", out var info)) continue;
            var title = page.GetProperty("title").GetString()!;
            var original = asked.GetValueOrDefault(title, title);
            var ii = info[0];
            var best = ii.TryGetProperty("thumburl", out var thumb) ? thumb.GetString() : ii.GetProperty("url").GetString();
            urls[original["File:".Length..]] = best!;
        }
        await Task.Delay(TimeSpan.FromSeconds(1));
    }
    return urls;
}

static async Task<byte[]> GetWithRetry(HttpClient http, string url)
{
    for (var attempt = 1; ; attempt++)
    {
        using var response = await http.GetAsync(url);
        if (response.IsSuccessStatusCode) return await response.Content.ReadAsByteArrayAsync();
        var retryable = response.StatusCode == HttpStatusCode.TooManyRequests || (int)response.StatusCode >= 500;
        if (!retryable || attempt == 4) throw new HttpRequestException($"HTTP {(int)response.StatusCode} for {url}");
        var wait = response.Headers.RetryAfter?.Delta ?? TimeSpan.FromSeconds(10 * attempt);
        Console.WriteLine($"  HTTP {(int)response.StatusCode}, waiting {wait.TotalSeconds:0}s before retrying...");
        await Task.Delay(wait);
    }
}

static string FindRepoRoot()
{
    for (var dir = new DirectoryInfo(Directory.GetCurrentDirectory()); dir is not null; dir = dir.Parent)
        if (File.Exists(Path.Combine(dir.FullName, "saint_portrait_sources.md"))) return dir.FullName;
    throw new DirectoryNotFoundException("Run this from inside the the-way-of-life folder (saint_portrait_sources.md not found).");
}

string? ArgValue(string name)
{
    var i = Array.IndexOf(args, name);
    return i >= 0 && i + 1 < args.Length ? args[i + 1] : null;
}
