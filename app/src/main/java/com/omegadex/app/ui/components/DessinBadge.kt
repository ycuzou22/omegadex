// Tracés recopiés de la maquette « Badges d'arène · originaux » (repère 64 × 64) : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.Rubis
import com.omegadex.app.ui.theme.RubisEclat
import kotlin.math.roundToInt

private const val REPERE = 64f
private val ENCRE = Color(0xFF141012)
private val OR_FONDU = Color(0xFFFFA040)
private val COEUR_FLAMME = Color(0xFFFFD15C)

/** Part de l'or fondu sur une image de badge encore brûlante (l'image reste lisible dessous). */
private const val OPACITE_FUSION = 0.85f

/** Un détail tracé par-dessus le corps du badge. */
private enum class TypeDetail { FACETTES, FILS, COEUR, SPIRALE }

private class Detail(val type: TypeDetail, val chemin: Path)

/** Gemme rubis incrustée : centre et rayon dans le repère 64 × 64. */
private class Incrustation(val x: Float, val y: Float, val rayon: Float)

/**
 * Dessin d'un badge : [corps] (aplats cernés de noir), [details], une [onde] facultative sous le corps
 * et la gemme rubis incrustée.
 */
private class DessinBadgeArene(
    val corps: List<Path>,
    val details: List<Detail> = emptyList(),
    val gemme: Incrustation,
    val onde: Rect? = null,
) {
    /** Union des pièces du corps : la zone où tombent l'ombre et les reflets. */
    val silhouette = Path().apply { corps.forEach { addPath(it) } }
}

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private fun cercle(x: Float, y: Float, r: Float) = Path().apply { addOval(Rect(Offset(x, y), r)) }

private val DESSINS: Map<BadgeArene, DessinBadgeArene> by lazy {
    mapOf(
        BadgeArene.ROCHE to DessinBadgeArene(
            corps = listOf(svg("M12 38 L18 16 L36 8 L52 18 L56 38 L42 54 L22 54 Z")),
            details = listOf(
                Detail(TypeDetail.FACETTES, svg("M18 16 L30 30 L36 8 M30 30 L52 18 M30 30 L42 54 M30 30 L12 38")),
            ),
            gemme = Incrustation(30f, 30f, 5f),
        ),
        BadgeArene.POING to DessinBadgeArene(
            corps = listOf(
                cercle(19f, 22f, 6f),
                cercle(28f, 20f, 6f),
                cercle(37f, 20f, 6f),
                cercle(46f, 22f, 6f),
                svg("M13 30 Q13 24 19 24 L46 24 Q52 24 52 30 L52 44 Q52 56 40 56 L25 56 Q13 56 13 44 Z"),
            ),
            details = listOf(Detail(TypeDetail.FACETTES, svg("M13 36 Q22 34 26 41"))),
            gemme = Incrustation(37f, 44f, 5f),
        ),
        BadgeArene.DYNAMO to DessinBadgeArene(
            corps = listOf(svg("M38 4 L14 36 L29 36 L22 60 L50 26 L34 26 L42 4 Z")),
            gemme = Incrustation(31f, 31f, 4.5f),
        ),
        BadgeArene.CHALEUR to DessinBadgeArene(
            corps = listOf(
                svg(
                    "M32 6 C40 18 52 24 50 40 C49 52 40 58 32 58 C24 58 15 52 14 40 C13 30 20 24 24 16 " +
                        "C26 24 30 26 32 28 C33 20 30 14 32 6 Z",
                ),
            ),
            details = listOf(
                Detail(TypeDetail.COEUR, svg("M32 32 C38 38 42 44 38 50 C36 54 28 54 26 50 C23 45 27 38 32 32 Z")),
            ),
            gemme = Incrustation(32f, 46f, 4.5f),
        ),
        BadgeArene.BALANCE to DessinBadgeArene(
            corps = listOf(
                svg("M29 16 L35 16 L35 48 L44 48 Q47 56 40 56 L24 56 Q17 56 20 48 L29 48 Z"),
                svg("M8 20 L56 20 L56 25 L8 25 Z"),
                svg("M5 38 Q14 50 23 38 Z"),
                svg("M41 38 Q50 50 59 38 Z"),
            ),
            details = listOf(Detail(TypeDetail.FILS, svg("M14 24 L8 38 M14 24 L20 38 M50 24 L44 38 M50 24 L56 38"))),
            gemme = Incrustation(32f, 14f, 5f),
        ),
        BadgeArene.PLUME to DessinBadgeArene(
            corps = listOf(svg("M54 8 C34 10 16 26 14 48 L18 52 C40 50 56 34 54 8 Z")),
            details = listOf(
                Detail(
                    TypeDetail.FACETTES,
                    svg(
                        "M52 11 L12 56 M40 20 L33 21 M45 27 L38 29 M31 31 L24 31 M36 38 L29 40 " +
                            "M25 44 L19 43 M46 34 L41 38",
                    ),
                ),
            ),
            gemme = Incrustation(17f, 51f, 4f),
        ),
        BadgeArene.ESPRIT to DessinBadgeArene(
            corps = listOf(cercle(32f, 32f, 22f)),
            details = listOf(
                Detail(
                    TypeDetail.SPIRALE,
                    svg("M32 29 A3 3 0 1 1 29 32 A6 6 0 1 1 35 38 A9 9 0 1 1 26 29 A12 12 0 1 1 38 41"),
                ),
            ),
            gemme = Incrustation(32f, 32f, 4f),
        ),
        BadgeArene.PLUIE to DessinBadgeArene(
            corps = listOf(svg("M32 4 C40 18 48 27 48 37 C48 46 41 53 32 53 C23 53 16 46 16 37 C16 27 24 18 32 4 Z")),
            gemme = Incrustation(32f, 39f, 5f),
            onde = Rect(10f, 53f, 54f, 61f),
        ),
    )
}

private val OMBRE = svg("M0 42 L64 24 L64 64 L0 64 Z")
private val REFLET = svg("M0 16 L38 0 L50 0 L0 24 Z")
private val REFLET_FIN = svg("M0 31 L58 0 L62 0 L0 34 Z")

/**
 * Dessine un badge d'arène dans toute la zone : l'[image] fournie (`assets/badges`, voir [ImagesBadges]) si elle
 * existe, sinon le badge original. [chaleur] (0 à 1) teinte le badge en métal en fusion, pour l'animation de forge.
 */
fun DrawScope.dessinerBadge(badge: BadgeArene, chaleur: Float = 0f, image: ImageBitmap? = null) {
    if (image != null) dessinerImageBadge(image, chaleur) else dessinerBadgeOriginal(badge, chaleur)
}

/** Image du badge centrée et réduite pour tenir dans la zone, recouverte d'or fondu selon [chaleur]. */
private fun DrawScope.dessinerImageBadge(image: ImageBitmap, chaleur: Float) {
    val echelle = minOf(size.width / image.width, size.height / image.height)
    val cible = IntSize((image.width * echelle).roundToInt(), (image.height * echelle).roundToInt())
    val coin = IntOffset((size.width.roundToInt() - cible.width) / 2, (size.height.roundToInt() - cible.height) / 2)
    drawImage(image, dstOffset = coin, dstSize = cible, filterQuality = FilterQuality.Medium)
    val c = chaleur.coerceIn(0f, 1f)
    if (c > 0f) {
        drawImage(
            image,
            dstOffset = coin,
            dstSize = cible,
            alpha = c * OPACITE_FUSION,
            colorFilter = ColorFilter.tint(OR_FONDU, BlendMode.SrcIn),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/**
 * Badge original, dessiné dans toute la zone (carrée de préférence), au rendu « pin's » : contour noir épais,
 * aplat à la couleur du type de l'arène, ombre et reflets en bandes diagonales, gemme rubis incrustée.
 */
private fun DrawScope.dessinerBadgeOriginal(badge: BadgeArene, chaleur: Float) {
    val dessin = DESSINS.getValue(badge)
    val couleur = lerp(CouleursTypes.de(badge.type), OR_FONDU, chaleur.coerceIn(0f, 1f))
    val echelle = size.minDimension / REPERE
    val decalage = Offset((size.width - REPERE * echelle) / 2f, (size.height - REPERE * echelle) / 2f)
    translate(decalage.x, decalage.y) {
        scale(echelle, pivot = Offset.Zero) {
            dessin.onde?.let {
                drawOval(ENCRE, it.topLeft, it.size, style = Stroke(4.5f))
                drawOval(couleur, it.topLeft, it.size, style = Stroke(2f))
            }
            dessin.corps.forEach { piece ->
                drawPath(piece, ENCRE, style = Stroke(6f, join = StrokeJoin.Round))
                drawPath(piece, couleur)
            }
            dessin.details.forEach { dessinerDetail(it) }
            clipPath(dessin.silhouette) {
                drawPath(OMBRE, Color.Black, alpha = 0.2f)
                drawPath(REFLET, Color.White, alpha = 0.5f)
                drawPath(REFLET_FIN, Color.White, alpha = 0.3f)
            }
            dessinerIncrustation(dessin.gemme)
        }
    }
}

private fun DrawScope.dessinerDetail(detail: Detail) {
    val trait = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (detail.type) {
        TypeDetail.FACETTES, TypeDetail.FILS -> drawPath(detail.chemin, ENCRE, style = trait)
        TypeDetail.COEUR -> {
            drawPath(detail.chemin, COEUR_FLAMME)
            drawPath(detail.chemin, ENCRE, style = Stroke(1.4f))
        }
        TypeDetail.SPIRALE -> drawPath(detail.chemin, Color.White, style = Stroke(2.4f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.dessinerIncrustation(gemme: Incrustation) {
    val centre = Offset(gemme.x, gemme.y)
    drawCircle(ENCRE, gemme.rayon + 0.8f, centre)
    drawCircle(Rubis, gemme.rayon, centre)
    drawCircle(RubisEclat, gemme.rayon * 0.6f, centre - Offset(1f, 1f))
    drawCircle(Color.White, gemme.rayon * 0.24f, centre - Offset(2f, 2f))
}
