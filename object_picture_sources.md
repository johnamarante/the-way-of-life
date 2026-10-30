# Picture Sources

Pictures for the "What is this?" questions (rules section 4.5) and the "Who is this?" statue and image questions (section 4.6), from Wikimedia Commons, where every file has a free license. Each object or person has 1 to 5 pictures; with more than one, the game shows a different one each time.

**How to use:** the files are downloaded and letterboxed to 256 x 256 by `tools/PortraitFetcher`:

```
dotnet run --project tools/PortraitFetcher -- --sources object_picture_sources.md --out objects
```

The "Save as" name must stay `<name>-<n>.jpg`, with the name from the `Picture` column in lowercase and spaces or symbols as underscores (`Brown Scapular` is `brown_scapular-1.jpg`). That is how the build finds the pictures. A picture of a person also says whether it is a statue or an image, which sets the wording of its question: `st_peter-statue-1.jpg`, `st_peter-image-2.jpg`.

**Credit needed:** public domain and CC0 images need nothing. Images marked **Yes** are under CC BY or CC BY-SA and are credited on the About screen; CC BY-SA also means the 256 x 256 copy is shared under the same license.

Licenses were read from Commons on 2026-10-30. Two pictures (marked "Supplied by you") were added by hand and are not from Commons; their source and license are not recorded. Every picture was checked by eye to show the object or person clearly, with nothing that could also fit one of the wrong answers.

| # | Object | Save as | Image | Size (px) | License | Credit needed | Notes |
|---|---|---|---|---|---|---|---|
| 1 | Monstrance | `monstrance-1.jpg` | [Custodia Iglesia Sn Ignacio](https://commons.wikimedia.org/wiki/File:Custodia_Iglesia_Sn_Ignacio.jpg) | 728 x 1388 | Public domain | No | |
| 2 | Monstrance | `monstrance-2.jpg` | [Anónimo limeño - Custodia (Museo de Arte de Lima (MALI),...](https://commons.wikimedia.org/wiki/File:An%C3%B3nimo_lime%C3%B1o_-_Custodia_%28Museo_de_Arte_de_Lima_%28MALI%29,_Lima%29.jpg) | 1000 x 1000 | CC BY 4.0 | Yes | |
| 3 | Monstrance | `monstrance-3.jpg` | [Custodia - Convento de San Carlos](https://commons.wikimedia.org/wiki/File:Custodia_-_Convento_de_San_Carlos.jpg) | 2508 x 3344 | CC BY-SA 4.0 | Yes | |
| 4 | Monstrance | `monstrance-4.jpg` | [Eucharistic Adoration - Monstrance](https://commons.wikimedia.org/wiki/File:Eucharistic_Adoration_-_Monstrance.jpg) | 1944 x 2592 | CC BY-SA 3.0 | Yes | |
| 5 | Tabernacle | `tabernacle-1.jpg` | [Kostel sv Vaclava Kostelec 012](https://commons.wikimedia.org/wiki/File:Kostel_sv_Vaclava_Kostelec_012.jpg) | 2288 x 3051 | CC BY 3.0 | Yes | |
| 6 | Tabernacle | `tabernacle-2.jpg` | [Bg-0631 Kristus-Kralj-tabernakelj](https://commons.wikimedia.org/wiki/File:Bg-0631_Kristus-Kralj-tabernakelj.jpg) | 2976 x 3968 | CC BY 4.0 | Yes | |
| 7 | Tabernacle | `tabernacle-3.jpg` | [G-Sakramentshaus-1](https://commons.wikimedia.org/wiki/File:G-Sakramentshaus-1.jpg) | 2979 x 3972 | CC BY-SA 3.0 | Yes | |
| 8 | Ciborium | `ciborium-1.jpg` | [2006.10 Copón](https://commons.wikimedia.org/wiki/File:2006.10_Cop%C3%B3n.jpg) | 1383 x 1844 | CC BY-SA 3.0 | Yes | |
| 9 | Ciborium | `ciborium-2.jpg` | [Ciborie (gruuthusemuseum) - anoniem - Musea Brugge - 1](https://commons.wikimedia.org/wiki/File:Ciborie_%28gruuthusemuseum%29_-_anoniem_-_Musea_Brugge_-_1.jpg) | 1500 x 2000 | Public domain | No | |
| 10 | Ciborium | `ciborium-3.jpg` | [Ciborie van zilver met vergulding - Joseph Jacquemart - M...](https://commons.wikimedia.org/wiki/File:Ciborie_van_zilver_met_vergulding_-_Joseph_Jacquemart_-_Musea_Brugge_-_1.jpg) | 1500 x 2000 | Public domain | No | |
| 11 | Pyx | `pyx-1.jpg` | [Bronze pyx](https://commons.wikimedia.org/wiki/File:Bronze_pyx.JPG) | 3264 x 2448 | CC BY-SA 3.0 | Yes | |
| 12 | Pyx | `pyx-2.jpg` | [Pyx MET sf32-100-282s4](https://commons.wikimedia.org/wiki/File:Pyx_MET_sf32-100-282s4.jpg) | 2448 x 3264 | CC0 | No | |
| 13 | Pyx | `pyx-3.jpg` | [Silver pyx MNMA Cl23528](https://commons.wikimedia.org/wiki/File:Silver_pyx_MNMA_Cl23528.jpg) | 1600 x 1680 | Public domain | No | |
| 14 | Chalice | `chalice-1.jpg` | [Dom Martin, Kelk (1931), KBS-FRB 01](https://commons.wikimedia.org/wiki/File:Dom_Martin,_Kelk_%281931%29,_KBS-FRB_01.jpg) | 2835 x 3969 | CC BY-SA 4.0 | Yes | |
| 15 | Chalice | `chalice-2.jpg` | [Chalice (Loreta) 01(js)](https://commons.wikimedia.org/wiki/File:Chalice_%28Loreta%29_01%28js%29.jpg) | 1588 x 2382 | CC BY-SA 3.0 | Yes | |
| 16 | Chalice | `chalice-3.jpg` | [Calice Max Stölb 88275](https://commons.wikimedia.org/wiki/File:Calice_Max_St%C3%B6lb_88275.jpg) | 1592 x 3272 | CC BY-SA 4.0 | Yes | |
| 17 | Chalice | `chalice-4.jpg` | [Sejny Muzeum kielich barokowy](https://commons.wikimedia.org/wiki/File:Sejny_Muzeum_kielich_barokowy.jpg) | 1361 x 2414 | CC BY-SA 3.0 | Yes | |
| 18 | Paten | `paten-2.jpg` | [Clevelandart 1990.3.2](https://commons.wikimedia.org/wiki/File:Clevelandart_1990.3.2.jpg) | 3400 x 3104 | CC0 | No | |
| 19 | Paten | `paten-3.jpg` | [Valle del reno, patena per la comunione, 1490-1500 ca](https://commons.wikimedia.org/wiki/File:Valle_del_reno,_patena_per_la_comunione,_1490-1500_ca.jpg) | 1928 x 1952 | CC BY 3.0 | Yes | |
| 20 | Corporal | `corporal-1.jpg` | [Korporale in cruce salus (1)](https://commons.wikimedia.org/wiki/File:Korporale_in_cruce_salus_%281%29.jpg) | 2560 x 1920 | CC BY-SA 4.0 | Yes | |
| 21 | Corporal | `corporal-2.jpg` | [Korporale in cruce salus (2)](https://commons.wikimedia.org/wiki/File:Korporale_in_cruce_salus_%282%29.jpg) | 2560 x 1920 | CC BY-SA 4.0 | Yes | |
| 22 | Purificator | `purificator-1.jpg` | [Purificatorium on chalice](https://commons.wikimedia.org/wiki/File:Purificatorium_on_chalice.JPG) | 652 x 488 | CC BY-SA 3.0 | Yes | |
| 23 | Purificator | `purificator-2.jpg` | [Purificator 3 Cieszyn Poland](https://commons.wikimedia.org/wiki/File:Purificator_3_Cieszyn_Poland.JPG) | 2816 x 2112 | CC BY-SA 4.0 | Yes | |
| 24 | Cruets | `cruets-1.jpg` | [Ampułki (1) - SACROEXPO-2013-06-17](https://commons.wikimedia.org/wiki/File:Ampu%C5%82ki_%281%29_-_SACROEXPO-2013-06-17.jpg) | 3456 x 2304 | CC BY-SA 3.0 | Yes | |
| 25 | Cruets | `cruets-2.jpg` | [MSM - Messkännchen einer Versehgarnitur](https://commons.wikimedia.org/wiki/File:MSM_-_Messk%C3%A4nnchen_einer_Versehgarnitur.jpg) | 3908 x 2568 | CC BY-SA 3.0 | Yes | |
| 26 | Cruets | `cruets-3.jpg` | [Clermont (60), église Saint-Samson, burettes, verre et mé...](https://commons.wikimedia.org/wiki/File:Clermont_%2860%29,_%C3%A9glise_Saint-Samson,_burettes,_verre_et_m%C3%A9tal_dor%C3%A9,_XIXe_si%C3%A8cle_1.jpg) | 5184 x 3456 | CC BY-SA 4.0 | Yes | |
| 27 | Cruets | `cruets-4.jpg` | [Meßkännchen, silber teilvergoldet](https://commons.wikimedia.org/wiki/File:Me%C3%9Fk%C3%A4nnchen,_silber_teilvergoldet.jpg) | 550 x 401 | CC BY-SA 3.0 | Yes | |
| 28 | Censer | `censer-1.jpg` | [1470 - Cadelnita de argint aurit](https://commons.wikimedia.org/wiki/File:1470_-_Cadelnita_de_argint_aurit.jpg) | 500 x 600 | Public domain | No | |
| 29 | Censer | `censer-2.jpg` | [Almunge kyrka rökelsekar 02](https://commons.wikimedia.org/wiki/File:Almunge_kyrka_r%C3%B6kelsekar_02.JPG) | 3264 x 4928 | CC BY-SA 3.0 | Yes | |
| 30 | Censer | `censer-3.jpg` | [Botafumeiro](https://commons.wikimedia.org/wiki/File:Botafumeiro.JPG) | 600 x 783 | CC BY 3.0 | Yes | |
| 31 | Censer | `censer-4.jpg` | [Antonio e paolo pini, turibolo, navicella e cucchiaio da...](https://commons.wikimedia.org/wiki/File:Antonio_e_paolo_pini,_turibolo,_navicella_e_cucchiaio_da_incenso,_1793,_in_rame_argentato_02.jpg) | 3060 x 3666 | CC BY-SA 4.0 | Yes | |
| 32 | Aspergillum & Aspersorium | `aspergillum_aspersorium-1.jpg` | [Gaetano guadagni, secchiello e aspersorio, 1822, argento 01](https://commons.wikimedia.org/wiki/File:Gaetano_guadagni,_secchiello_e_aspersorio,_1822,_argento_01.JPG) | 2312 x 1613 | CC BY 3.0 | Yes | |
| 33 | Aspergillum & Aspersorium | `aspergillum_aspersorium-2.jpg` | [Bottega toscana, secchiello per acqua benedetta e asperso...](https://commons.wikimedia.org/wiki/File:Bottega_toscana,_secchiello_per_acqua_benedetta_e_aspersorio,_in_metallo_argentato,_xx_secolo.jpg) | 2826 x 3840 | CC BY 3.0 | Yes | |
| 34 | Aspergillum & Aspersorium | `aspergillum_aspersorium-3.jpg` | [Abraham pfleger (attr.), secchiello e aspersorio, augusta...](https://commons.wikimedia.org/wiki/File:Abraham_pfleger_%28attr.%29,_secchiello_e_aspersorio,_augusta_1577.jpg) | 1800 x 2127 | CC BY 3.0 | Yes | |
| 35 | Crucifix | `crucifix-1.jpg` | [Christ janséniste](https://commons.wikimedia.org/wiki/File:Christ_jans%C3%A9niste.JPG) | 1536 x 2048 | Public domain | No | |
| 36 | Crucifix | `crucifix-2.jpg` | [Art de tranché, 2](https://commons.wikimedia.org/wiki/File:Art_de_tranch%C3%A9,_2.jpg) | 4004 x 6000 | Public domain | No | |
| 37 | Crucifix | `crucifix-3.jpg` | [Abuabu cross moulded 1928](https://commons.wikimedia.org/wiki/File:Abuabu_cross_moulded_1928.jpg) | 483 x 480 | CC BY-SA 4.0 | Yes | |
| 38 | Crucifix | `crucifix-4.jpg` | [A wooden crucifix](https://commons.wikimedia.org/wiki/File:A_wooden_crucifix.jpg) | 6024 x 4024 | CC0 | No | |
| 39 | Rosary | `rosary-1.jpg` | [Art de tranché, 1](https://commons.wikimedia.org/wiki/File:Art_de_tranch%C3%A9,_1.jpg) | 6000 x 4004 | Public domain | No | |
| 40 | Rosary | `rosary-2.jpg` | [2023 Różaniec](https://commons.wikimedia.org/wiki/File:2023_R%C3%B3%C5%BCaniec.jpg) | 4416 x 2599 | CC BY-SA 4.0 | Yes | |
| 41 | Rosary | `rosary-3.jpg` | [02014 Der Gebetskette-Rosenkranz aus Pimpernuß. Die beski...](https://commons.wikimedia.org/wiki/File:02014_Der_Gebetskette-Rosenkranz_aus_Pimpernu%C3%9F._Die_beskidische_Samenk%C3%B6rner_mit_Silberdraht_gefa%C3%9Ft..JPG) | 2947 x 2949 | CC BY-SA 4.0 | Yes | |
| 42 | Brown Scapular | `brown_scapular-1.jpg` | [Brown scapular showing the Blessed Virgin of Mount Carmel...](https://commons.wikimedia.org/wiki/File:Brown_scapular_showing_the_Blessed_Virgin_of_Mount_Carmel,_E_Wellcome_L0058985.jpg) | 2716 x 4106 | CC BY 4.0 | Yes | |
| 43 | Brown Scapular | `brown_scapular-2.jpg` | [Scapulaire nd mont carmel](https://commons.wikimedia.org/wiki/File:Scapulaire_nd_mont_carmel.jpg) | 2712 x 2640 | CC BY-SA 4.0 | Yes | |
| 44 | Brown Scapular | `brown_scapular-3.jpg` | [Escapulariocafe](https://commons.wikimedia.org/wiki/File:Escapulariocafe.JPG) | 2318 x 1662 | CC BY-SA 3.0 | Yes | |
| 45 | Miraculous Medal | `miraculous_medal-1.jpg` | [Miraculous medal](https://commons.wikimedia.org/wiki/File:Miraculous_medal.jpg) | 2540 x 1800 | CC BY-SA 3.0 | Yes | |
| 46 | Miraculous Medal | `miraculous_medal-2.jpg` | [Miraculous medal (4)](https://commons.wikimedia.org/wiki/File:Miraculous_medal_%284%29.jpg) | 3388 x 2794 | CC0 | No | |
| 47 | Miraculous Medal | `miraculous_medal-3.jpg` | [Miraculous medal (3)](https://commons.wikimedia.org/wiki/File:Miraculous_medal_%283%29.jpg) | 4025 x 3448 | CC0 | No | |
| 48 | Medal of Saint Benedict | `medal_of_saint_benedict-1.jpg` | [Benediktusmedaille 2](https://commons.wikimedia.org/wiki/File:Benediktusmedaille_2.JPG) | 992 x 994 | Public domain | No | |
| 49 | Medal of Saint Benedict | `medal_of_saint_benedict-2.jpg` | [Avers d'une médaille de Saint Benoît (juillet 2022)](https://commons.wikimedia.org/wiki/File:Avers_d'une_m%C3%A9daille_de_Saint_Beno%C3%AEt_%28juillet_2022%29.JPG) | 2178 x 2121 | CC0 | No | |
| 50 | Medal of Saint Benedict | `medal_of_saint_benedict-3.jpg` | [Medalha de Sao Bento 2](https://commons.wikimedia.org/wiki/File:Medalha_de_Sao_Bento_2.jpg) | 4224 x 2816 | CC BY-SA 3.0 | Yes | |
| 51 | Paschal candle | `paschal_candle-2.jpg` | [Osterkerze Ostervigil 2023](https://commons.wikimedia.org/wiki/File:Osterkerze_Ostervigil_2023.jpg) | 2576 x 3872 | CC BY-SA 4.0 | Yes | |
| 52 | Baptismal candle | `baptismal_candle-1.jpg` | [Vela en un bautismo católico](https://commons.wikimedia.org/wiki/File:Vela_en_un_bautismo_cat%C3%B3lico.JPG) | 5087 x 3456 | CC BY-SA 4.0 | Yes | |
| 53 | Baptismal candle | `baptismal_candle-2.jpg` | [Taufkerze KreuzmitRosenranke](https://commons.wikimedia.org/wiki/File:Taufkerze_KreuzmitRosenranke.jpg) | 562 x 840 | CC BY 4.0 | Yes | |
| 54 | Baptismal candle | `baptismal_candle-3.jpg` | [Taufkerze Sommer](https://commons.wikimedia.org/wiki/File:Taufkerze_Sommer.jpg) | 563 x 840 | CC BY 4.0 | Yes | |
| 55 | Altar | `altar-1.jpg` | [Altar at Church of the Assumption, Thomastown](https://commons.wikimedia.org/wiki/File:Altar_at_Church_of_the_Assumption,_Thomastown.jpg) | 4032 x 2016 | CC BY-SA 4.0 | Yes | |
| 56 | Altar | `altar-2.jpg` | [Dülmen, Heilig-Kreuz-Kirche, Altar -- 2019 -- 3113-7](https://commons.wikimedia.org/wiki/File:D%C3%BClmen,_Heilig-Kreuz-Kirche,_Altar_--_2019_--_3113-7.jpg) | 6034 x 4479 | CC BY-SA 4.0 | Yes | |
| 57 | Altar | `altar-3.jpg` | [Freistadt Pfarrkirche Nothelferaltar 01](https://commons.wikimedia.org/wiki/File:Freistadt_Pfarrkirche_Nothelferaltar_01.jpg) | 3723 x 5585 | CC BY-SA 4.0 | Yes | |
| 58 | Ambo | `ambo-1.jpg` | [Gumpersdorf St Ursulastr Kirche StUrsula 015 201509 333](https://commons.wikimedia.org/wiki/File:Gumpersdorf_St_Ursulastr_Kirche_StUrsula_015_201509_333.JPG) | 3240 x 4320 | CC BY-SA 4.0 | Yes | |
| 59 | Ambo | `ambo-2.jpg` | [Emmerichskirche Ambo](https://commons.wikimedia.org/wiki/File:Emmerichskirche_Ambo.jpg) | 3024 x 4032 | CC0 | No | |
| 60 | Ambo | `ambo-3.jpg` | [Catedral de Medellín-Ambon](https://commons.wikimedia.org/wiki/File:Catedral_de_Medell%C3%ADn-Ambon.JPG) | 1944 x 2246 | CC BY-SA 2.5 | Yes | |
| 61 | Ambo | `ambo-4.jpg` | [Basilica of St. Michael the Archangel - Loretto, Pennsylv...](https://commons.wikimedia.org/wiki/File:Basilica_of_St._Michael_the_Archangel_-_Loretto,_Pennsylvania_12.jpg) | 4608 x 3456 | CC BY-SA 4.0 | Yes | |
| 62 | Mitre | `mitre-1.jpg` | [Gebhardsmitra](https://commons.wikimedia.org/wiki/File:Gebhardsmitra.jpg) | 567 x 716 | CC BY-SA 3.0 | Yes | |
| 63 | Mitre | `mitre-2.jpg` | [Mitre Pierre Simon de Dreux-Brézé Moulins](https://commons.wikimedia.org/wiki/File:Mitre_Pierre_Simon_de_Dreux-Br%C3%A9z%C3%A9_Moulins.jpg) | 5081 x 7621 | Public domain | No | |
| 64 | Mitre | `mitre-3.jpg` | [Late nineteenth century mitre in the treasury of the Cath...](https://commons.wikimedia.org/wiki/File:Late_nineteenth_century_mitre_in_the_treasury_of_the_Cathedral_of_Trier.jpg) | 1173 x 1371 | CC0 | No | |
| 65 | Mitre | `mitre-4.jpg` | [Thierry Jordan 06713 (cropped)](https://commons.wikimedia.org/wiki/File:Thierry_Jordan_06713_%28cropped%29.jpg) | 2730 x 2721 | CC BY-SA 3.0 | Yes | |
| 66 | Stole | `stole-1.jpg` | [Vavřinec Skýpala, Sv. Maří, 2017 (01)](https://commons.wikimedia.org/wiki/File:Vav%C5%99inec_Sk%C3%BDpala,_Sv._Ma%C5%99%C3%AD,_2017_%2801%29.jpg) | 753 x 1065 | CC BY-SA 4.0 | Yes | |
| 67 | Stole | `stole-2.jpg` | [Leonas Kalinauskas](https://commons.wikimedia.org/wiki/File:Leonas_Kalinauskas.JPG) | 768 x 1024 | CC BY-SA 3.0 | Yes | |
| 68 | Vestments | `vestments-1.jpg` | [Casulas na Igreja do Carmo em Pirenópolis](https://commons.wikimedia.org/wiki/File:Casulas_na_Igreja_do_Carmo_em_Piren%C3%B3polis.jpg) | 3216 x 4288 | CC BY-SA 3.0 | Yes | |
| 69 | Vestments | `vestments-2.jpg` | [130 C.H. de Vries](https://commons.wikimedia.org/wiki/File:130_C.H._de_Vries.jpg) | 515 x 786 | CC BY-SA 3.0 | Yes | |
| 70 | Vestments | `vestments-3.jpg` | [Fr. Antony Sumich FSSP](https://commons.wikimedia.org/wiki/File:Fr._Antony_Sumich_FSSP.jpg) | 1500 x 1000 | CC BY-SA 4.0 | Yes | |
| 71 | Vestments | `vestments-4.jpg` | [131 Chasuble Plouigneau](https://commons.wikimedia.org/wiki/File:131_Chasuble_Plouigneau.jpg) | 1512 x 2244 | CC BY-SA 4.0 | Yes | |
| 72 | Confessional | `confessional-1.jpg` | [Kappelwindeck-St Maria-68-Beichtstuhl-2023-gje](https://commons.wikimedia.org/wiki/File:Kappelwindeck-St_Maria-68-Beichtstuhl-2023-gje.jpg) | 3072 x 3813 | CC BY 4.0 | Yes | |
| 73 | Confessional | `confessional-2.jpg` | [St Blaise church in Limogne-en-Quercy 12](https://commons.wikimedia.org/wiki/File:St_Blaise_church_in_Limogne-en-Quercy_12.jpg) | 3563 x 4879 | CC BY-SA 4.0 | Yes | |
| 74 | Confessional | `confessional-3.jpg` | [Confessional door open](https://commons.wikimedia.org/wiki/File:Confessional_door_open.jpg) | 2448 x 3264 | CC BY-SA 3.0 | Yes | |
| 75 | Confessional | `confessional-4.jpg` | [St Maurice church in La Jonchere-St-Maurice 03](https://commons.wikimedia.org/wiki/File:St_Maurice_church_in_La_Jonchere-St-Maurice_03.jpg) | 3470 x 4940 | CC BY-SA 4.0 | Yes | |
| 76 | Communion rail | `communion_rail-1.jpg` | [Kirche Unterdürnbach Kommunionbank](https://commons.wikimedia.org/wiki/File:Kirche_Unterd%C3%BCrnbach_Kommunionbank.jpg) | 3008 x 2000 | CC BY-SA 3.0 at | Yes | |
| 77 | Communion rail | `communion_rail-2.jpg` | [KortenbosBasiliek, interieur02](https://commons.wikimedia.org/wiki/File:KortenbosBasiliek,_interieur02.jpg) | 2294 x 1249 | CC BY-SA 3.0 | Yes | |
| 78 | Communion rail | `communion_rail-3.jpg` | [4897Religious of the Virgin Mary 04](https://commons.wikimedia.org/wiki/File:4897Religious_of_the_Virgin_Mary_04.jpg) | 4608 x 3456 | CC0 | No | |

## People

| # | Person | Save as | Image | Size (px) | License | Credit needed | Notes |
|---|---|---|---|---|---|---|---|
| 79 | Jesus | `jesus-image-2.jpg` | [SacredHeartBatoni](https://commons.wikimedia.org/wiki/File:SacredHeartBatoni.jpg) | 1034 x 1441 | CC0 | No | |
| 80 | Jesus | `jesus-statue-1.jpg` | Supplied by you (local file, not from Commons) | 256 x 256 | Unknown | Check | Statue of Christ with the cross on St. Peter's Basilica |
| 81 | The Blessed Virgin Mary | `the_blessed_virgin_mary-statue-1.jpg` | [Statue Virgin Mary Monastery San Hieronimo, Granada, Anda...](https://commons.wikimedia.org/wiki/File:Statue_Virgin_Mary_Monastery_San_Hieronimo,_Granada,_Andalusia,_Spain.jpg) | 2675 x 4423 | CC0 | No | |
| 82 | The Blessed Virgin Mary | `the_blessed_virgin_mary-statue-2.jpg` | [Statue of the Blessed Virgin Mary at Saint Anthony Cathol...](https://commons.wikimedia.org/wiki/File:Statue_of_the_Blessed_Virgin_Mary_at_Saint_Anthony_Catholic_Church_in_Helper_Utah.jpg) | 1836 x 3264 | CC BY-SA 4.0 | Yes | |
| 83 | The Blessed Virgin Mary | `the_blessed_virgin_mary-statue-4.jpg` | [Virgin Mary statue, St Mary's Catholic Church, Northcote](https://commons.wikimedia.org/wiki/File:Virgin_Mary_statue,_St_Mary's_Catholic_Church,_Northcote.jpg) | 3841 x 6000 | CC BY 4.0 | Yes | |
| 84 | Our Lady of Guadalupe | `our_lady_of_guadalupe-image-1.jpg` | [Virgen de guadalupe1](https://commons.wikimedia.org/wiki/File:Virgen_de_guadalupe1.jpg) | 788 x 1226 | Public domain | No | |
| 85 | Our Lady of Lourdes | `our_lady_of_lourdes-statue-4.jpg` | [Virgen de Lourdes](https://commons.wikimedia.org/wiki/File:Virgen_de_Lourdes.png) | 1536 x 2048 | CC BY-SA 4.0 | Yes | |
| 86 | Our Lady of Fatima | `our_lady_of_fatima-statue-1.jpg` | [Our Lady of Fatima Statuette](https://commons.wikimedia.org/wiki/File:Our_Lady_of_Fatima_Statuette.JPG) | 2304 x 3072 | CC BY-SA 2.5 nl | Yes | |
| 87 | St. Bernadette | `st_bernadette-image-1.jpg` | [Bernadette Soubirous](https://commons.wikimedia.org/wiki/File:Bernadette_Soubirous.jpg) | 1273 x 1643 | Public domain | No | |
| 88 | St. Junipero Serra | `st_junipero_serra-statue-2.jpg` | [2015-09-18 Mission San Carlos Borromeo de Carmelo 05](https://commons.wikimedia.org/wiki/File:2015-09-18_Mission_San_Carlos_Borromeo_de_Carmelo_05.jpg) | 1467 x 2200 | CC BY-SA 4.0 | Yes | |
| 89 | St. Junipero Serra | `st_junipero_serra-statue-3.jpg` | [Father Junipero Serra](https://commons.wikimedia.org/wiki/File:Father_Junipero_Serra.jpg) | 3648 x 5472 | CC BY-SA 3.0 | Yes | |
| 90 | St. Thomas More | `st_thomas_more-image-1.jpg` | [Sir Thomas More by Hans Holbein the Younger](https://commons.wikimedia.org/wiki/File:Sir_Thomas_More_by_Hans_Holbein_the_Younger.jpg) | 2400 x 3134 | Public domain | No | |
| 91 | Our Lady of Perpetual Help | `our_lady_of_perpetual_help-image-1.jpg` | [Perpetual help original icon](https://commons.wikimedia.org/wiki/File:Perpetual_help_original_icon.jpg) | 500 x 609 | Public domain | No | |
| 92 | St. Joseph | `st_joseph-statue-1.jpg` | [Joseph (Ciney, église)](https://commons.wikimedia.org/wiki/File:Joseph_%28Ciney,_%C3%A9glise%29.JPG) | 3456 x 4608 | CC BY-SA 4.0 | Yes | |
| 93 | The Holy Family | `the_holy_family-statue-1.jpg` | [Lazkao - Monasterio de Santa Ana (MM Cistercienses) 25](https://commons.wikimedia.org/wiki/File:Lazkao_-_Monasterio_de_Santa_Ana_%28MM_Cistercienses%29_25.jpg) | 2048 x 1536 | Public domain | No | |
| 94 | The Holy Family | `the_holy_family-statue-2.jpg` | [Holy Family Catholic Church (North Baltimore, Ohio) - Hol...](https://commons.wikimedia.org/wiki/File:Holy_Family_Catholic_Church_%28North_Baltimore,_Ohio%29_-_Holy_Family_statues,_detail.jpg) | 1913 x 1440 | CC BY-SA 3.0 | Yes | |
| 95 | The Holy Family | `the_holy_family-statue-4.jpg` | [The Holy Family at Corpus Christi Catholic Church in Frui...](https://commons.wikimedia.org/wiki/File:The_Holy_Family_at_Corpus_Christi_Catholic_Church_in_Fruitland_Idaho.jpg) | 1289 x 1992 | CC BY-SA 4.0 | Yes | |
| 96 | St. Peter | `st_peter-image-1.jpg` | [Pope-peter pprubens](https://commons.wikimedia.org/wiki/File:Pope-peter_pprubens.jpg) | 2318 x 3051 | Public domain | No | Rubens |
| 97 | St. Peter | `st_peter-statue-2.jpg` | Supplied by you (local file, not from Commons) | 800 x 800 | Unknown | Check | Statue of St. Peter with the keys, St. Peter's Square |
| 98 | St. Paul | `st_paul-statue-1.jpg` | [Apostel Paulus Pankratiuskirche Dossenheim](https://commons.wikimedia.org/wiki/File:Apostel_Paulus_Pankratiuskirche_Dossenheim.JPG) | 2736 x 3648 | CC BY-SA 3.0 | Yes | |
| 99 | St. Paul | `st_paul-statue-2.jpg` | [Reute Pfarrkirche Chor Hl Paulus](https://commons.wikimedia.org/wiki/File:Reute_Pfarrkirche_Chor_Hl_Paulus.jpg) | 1168 x 1797 | CC BY 3.0 | Yes | |
| 100 | St. Michael the Archangel | `st_michael_the_archangel-image-2.jpg` | [Hadziewicz, Rafał (1803-1886) - Saint Michael the Archang...](https://commons.wikimedia.org/wiki/File:Hadziewicz,_Rafa%C5%82_%281803-1886%29_-_Saint_Michael_the_Archangel_Vanquishing_Satan_-_MNK_XII-A-24_-_National_Museum_Krak%C3%B3w.jpg) | 3051 x 4000 | Public domain | No | |
| 101 | St. George | `st_george-statue-4.jpg` | [Saint george2](https://commons.wikimedia.org/wiki/File:Saint_george2.jpg) | 1668 x 2274 | CC BY-SA 3.0 | Yes | |
| 102 | St. Patrick | `st_patrick-image-3.jpg` | [Saint Patrick Catholic Church (Junction City, Ohio) - sta...](https://commons.wikimedia.org/wiki/File:Saint_Patrick_Catholic_Church_%28Junction_City,_Ohio%29_-_stained_glass,_Saint_Patrick_-_detail.jpg) | 2388 x 2991 | CC BY-SA 4.0 | Yes | |
| 103 | St. John the Baptist | `st_john_the_baptist-statue-1.jpg` | [Inquisitor Palace Birgu 2012 n14](https://commons.wikimedia.org/wiki/File:Inquisitor_Palace_Birgu_2012_n14.jpg) | 2364 x 4135 | CC BY 2.5 | Yes | |
