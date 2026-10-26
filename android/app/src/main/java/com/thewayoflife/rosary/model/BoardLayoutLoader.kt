package com.thewayoflife.rosary.model

import android.content.res.AssetManager
import org.json.JSONObject

class BoardLayoutLoader(private val assets: AssetManager) {
    fun load(assetName: String = "rosary_beads.json"): BoardLayout {
        val root = JSONObject(assets.open(assetName).bufferedReader().use { it.readText() })
        val beadArray = root.getJSONArray("beads")
        val beads = buildList(beadArray.length()) {
            for (position in 0 until beadArray.length()) {
                val item = beadArray.getJSONObject(position)
                add(
                    Bead(
                        index = item.getInt("index"),
                        type = BeadType.valueOf(item.getString("type").uppercase()),
                        x = item.getDouble("x").toFloat(),
                        y = item.getDouble("y").toFloat(),
                        radius = item.getDouble("r").toFloat(),
                    ),
                )
            }
        }
        return BoardLayout(
            image = root.getString("image"),
            imageWidth = root.getInt("imageWidth"),
            imageHeight = root.getInt("imageHeight"),
            beads = beads,
        )
    }
}
