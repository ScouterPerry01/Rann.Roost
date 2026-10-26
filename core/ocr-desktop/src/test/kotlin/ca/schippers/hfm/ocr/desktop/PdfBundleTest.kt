// File path and name: core/ocr-desktop/src/test/kotlin/ca/schippers/hfm/ocr/desktop/PdfBundleTest.kt
// Modified On Timestamp: 2026-10-25 @ 21:13 EDT
// Created On Timestamp: 2026-10-02 @ 21:26 EDT
// File Description: Tests of the PDFs the desktop writes: receipt bundles, multi-page captures, the home
//   inventory and the property fact sheet.
// Uses: PdfPages and DocumentReader; Apache PDFBox to read the results back.
// Used By: ./gradlew :core:ocr-desktop:test
// Purpose: Keep the generated PDFs readable, complete and free of characters the fonts lack.
package ca.schippers.hfm.ocr.desktop

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfBundleTest {

    private fun jpeg(): ByteArray = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(400, 600, BufferedImage.TYPE_INT_RGB), "jpg", it) }.toByteArray()

    @Test
    fun `receipts bundle with a cover page`() {
        val pdf = PdfPages.fromJpegs(listOf(jpeg(), jpeg()))
        val bundle = PdfPages.bundle(
            "Frais médicaux 2026",
            listOf("2026-03-01  Léa  Examen de la vue  90,00 $", "Unsupported 漢字 text"),
            listOf("application/pdf" to pdf, "image/jpeg" to jpeg()),
        )
        Loader.loadPDF(bundle).use { doc ->
            assertEquals(4, doc.numberOfPages, "cover, the two pages of the PDF, and the photo")
            val text = PDFTextStripper().apply { startPage = 1; endPage = 1 }.getText(doc)
            assertTrue("Frais médicaux 2026" in text)
            assertTrue("Examen de la vue" in text)
            assertTrue("Unsupported ?? text" in text, "characters the font lacks are replaced")
        }
    }

    @Test
    fun `every page of a multi-page capture can be shown, one at a time`() {
        // A two-page bill photographed on the phone arrives as one PDF of its pages.
        val pdf = PdfPages.fromJpegs(listOf(jpeg(), ByteArrayOutputStream().also { ImageIO.write(BufferedImage(500, 300, BufferedImage.TYPE_INT_RGB), "jpg", it) }.toByteArray()))
        assertEquals(2, DocumentReader.pageCount(pdf))
        val first = DocumentReader.page(pdf, 0)!!
        val second = DocumentReader.page(pdf, 1)!!
        assertTrue(first.height > first.width, "the first page is the tall photo")
        assertTrue(second.width > second.height, "the second page is the wide photo, not the first again")
        assertEquals(null, DocumentReader.page(pdf, 2))
        assertEquals(null, DocumentReader.page(pdf, -1))
        // An image is one page; a large photo is scaled down for the viewer.
        val photo = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(4000, 3000, BufferedImage.TYPE_INT_RGB), "jpg", it) }.toByteArray()
        assertEquals(1, DocumentReader.pageCount(photo))
        val shown = DocumentReader.page(photo, 0)!!
        assertTrue(shown.width in 1000..2400 && shown.width > shown.height, "${shown.width} x ${shown.height}")
        assertEquals(null, DocumentReader.page(photo, 1))
        assertEquals(0, DocumentReader.pageCount("plain text".encodeToByteArray()))
    }

    @Test
    fun `home inventory with photos`() {
        val items = (1..12).map { n -> listOf("Item $n", "Salon · 1 200,00 $") to listOf(jpeg(), jpeg()) }
        val pdf = PdfPages.inventory("Inventaire du domicile", listOf("12 articles"), items)
        Loader.loadPDF(pdf).use { doc ->
            assertTrue(doc.numberOfPages > 1, "photos spill onto more pages")
            val text = PDFTextStripper().getText(doc)
            assertTrue("Inventaire du domicile" in text && "Item 12" in text)
        }
    }

    @Test
    fun `a property fact sheet wraps long values and runs onto more pages`() {
        val legal = (1..40).joinToString(" ") { "Part $it of Lot 27, Plan 4M-1182;" }
        val rooms = (1..60).map { "Bedroom $it" to "Second floor · 11 ft × 10 ft · 110 sq ft · Hardwood" }
        val pdf = PdfPages.factSheet(
            "Fiche descriptive de la propriété : Maison familiale",
            listOf("Maison", "48 Maple Crescent, Ottawa ON  K2B 4N7", "Préparée le 2026-10-25"),
            listOf(
                "Désignation et titre" to listOf("Désignation cadastrale complète" to legal),
                // French amounts group with narrow no-break spaces and end with a no-break space before $.
                "Valeur et taxes" to listOf("Valeur au rôle" to "591 000,00 $"),
                "Pièces" to rooms,
            ),
        )
        Loader.loadPDF(pdf).use { doc ->
            assertTrue(doc.numberOfPages >= 2, "sixty rooms do not fit on one page")
            val text = PDFTextStripper().getText(doc)
            assertTrue("591 000,00 $" in text, "no-break spaces print as spaces, not question marks")
            assertTrue("Part 40 of Lot 27" in text && "Bedroom 60" in text, "nothing is cut off")
            val longest = text.lines().maxOf { it.length }
            assertTrue(longest < 140, "the long legal description is wrapped (longest line $longest characters)")
        }
    }
}
