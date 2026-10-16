package com.jarvis.app.code

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.core.graphics.PathParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real file generators that run on the phone: PDF and PNG are DRAWN (shapes, gradients, icons, vector paths, text, tables, images),
 * DOCX, XLSX and PPTX are written as proper Office files. The AI describes the file as JSON (see the "documents" skill).
 */
object DocGen {
    /** Material-style 24x24 icons that can be drawn anywhere with {"type":"icon","name":"mail"}. */
    val ICONS = mapOf(
        "check" to "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
        "close" to "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
        "star" to "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z",
        "heart" to "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z",
        "mail" to "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z",
        "phone" to "M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z",
        "home" to "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z",
        "person" to "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
        "lock" to "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z",
        "arrow" to "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z",
        "search" to "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
        "bolt" to "M7 2v11h3v9l7-12h-4l4-8z",
        "download" to "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z",
        "cloud" to "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z",
        "calendar" to "M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z",
        "location" to "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"
    )

    private fun col(s: String?, def: Int = Color.BLACK): Int = try { if (s.isNullOrBlank()) def else Color.parseColor(s) } catch (e: Exception) { def }
    private fun hex6(s: String?, def: String = "000000"): String = try { String.format("%06X", Color.parseColor(s) and 0xFFFFFF) } catch (e: Exception) { def }

    // ---------------------------------------------------------------- PDF / PNG drawing
    private fun pagesOf(spec: JSONObject): JSONArray = spec.optJSONArray("pages") ?: JSONArray().put(spec)

    private fun fillPaint(o: JSONObject, x: Float, y: Float, w: Float, h: Float): Paint? {
        val g = o.optJSONObject("gradient")
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        if (g != null) {
            val a = Math.toRadians(g.optDouble("angle", 90.0))
            val cx = x + w / 2; val cy = y + h / 2
            val dx = (cos(a) * w / 2).toFloat(); val dy = (sin(a) * h / 2).toFloat()
            p.shader = LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, col(g.optString("from"), Color.WHITE), col(g.optString("to"), Color.BLACK), Shader.TileMode.CLAMP)
            return p
        }
        if (!o.has("fill")) return null
        p.color = col(o.optString("fill")); return p
    }

    private fun strokePaint(o: JSONObject): Paint? {
        if (!o.has("stroke")) return null
        return Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = col(o.optString("stroke")); strokeWidth = o.optDouble("strokeWidth", 1.0).toFloat() }
    }

    private fun typeface(o: JSONObject): Typeface {
        val base = when (o.optString("font", "sans")) { "serif" -> Typeface.SERIF; "mono" -> Typeface.MONOSPACE; else -> Typeface.SANS_SERIF }
        val style = (if (o.optBoolean("bold")) Typeface.BOLD else 0) or (if (o.optBoolean("italic")) Typeface.ITALIC else 0)
        return Typeface.create(base, style)
    }

    private fun drawText(c: Canvas, o: JSONObject, text: String, x: Float, y: Float, w: Float) {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = col(o.optString("color"), Color.BLACK); textSize = o.optDouble("size", 14.0).toFloat(); typeface = typeface(o) }
        val align = when (o.optString("align", "left")) { "center" -> Layout.Alignment.ALIGN_CENTER; "right" -> Layout.Alignment.ALIGN_OPPOSITE; else -> Layout.Alignment.ALIGN_NORMAL }
        val l = StaticLayout.Builder.obtain(text, 0, text.length, tp, w.toInt().coerceAtLeast(10)).setAlignment(align).setLineSpacing(0f, o.optDouble("lineHeight", 1.15).toFloat()).build()
        c.save(); c.translate(x, y); l.draw(c); c.restore()
    }

    private fun drawElement(c: Canvas, o: JSONObject, pageW: Float) {
        val x = o.optDouble("x", 0.0).toFloat(); val y = o.optDouble("y", 0.0).toFloat()
        when (o.optString("type")) {
            "rect" -> {
                val w = o.optDouble("w").toFloat(); val h = o.optDouble("h").toFloat(); val r = o.optDouble("radius", 0.0).toFloat()
                val rect = RectF(x, y, x + w, y + h)
                fillPaint(o, x, y, w, h)?.let { c.drawRoundRect(rect, r, r, it) }
                strokePaint(o)?.let { c.drawRoundRect(rect, r, r, it) }
            }
            "circle" -> {
                val r = o.optDouble("r", 10.0).toFloat()
                fillPaint(o, x - r, y - r, 2 * r, 2 * r)?.let { c.drawCircle(x, y, r, it) }
                strokePaint(o)?.let { c.drawCircle(x, y, r, it) }
            }
            "line" -> c.drawLine(o.optDouble("x1").toFloat(), o.optDouble("y1").toFloat(), o.optDouble("x2").toFloat(), o.optDouble("y2").toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col(o.optString("color"), Color.BLACK); strokeWidth = o.optDouble("width", 1.0).toFloat(); strokeCap = Paint.Cap.ROUND })
            "text" -> drawText(c, o, o.optString("text"), x, y, o.optDouble("w", (pageW - x - 24).toDouble()).toFloat())
            "path", "icon" -> {
                val isIcon = o.optString("type") == "icon"
                val d = if (isIcon) ICONS[o.optString("name")] ?: throw IOException("Unknown icon \"${o.optString("name")}\". Icons: ${ICONS.keys.joinToString()}") else o.optString("d")
                val scale = if (isIcon) (o.optDouble("size", 24.0) / 24.0).toFloat() else o.optDouble("scale", 1.0).toFloat()
                val path = Path(PathParser.createPathFromPathData(d) ?: throw IOException("The path data is not valid SVG path data"))
                c.save(); c.translate(x, y); c.scale(scale, scale)
                val f = if (isIcon) Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col(o.optString("color"), Color.BLACK) } else fillPaint(o, 0f, 0f, 24f, 24f)
                f?.let { c.drawPath(path, it) }
                strokePaint(o)?.let { c.drawPath(path, it) }
                c.restore()
            }
            "image" -> {
                val bytes = Base64.decode(o.optString("data").substringAfter(","), Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Image data could not be read")
                val w = o.optDouble("w", bmp.width.toDouble()).toFloat(); val h = o.optDouble("h", bmp.height.toDouble()).toFloat()
                c.drawBitmap(bmp, null, RectF(x, y, x + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
            }
            "table" -> {
                val rows = o.optJSONArray("rows") ?: return
                val widths = o.optJSONArray("colWidths")
                val ncol = (rows.optJSONArray(0)?.length() ?: 1)
                val rh = o.optDouble("rowHeight", 28.0).toFloat()
                val cw = FloatArray(ncol) { widths?.optDouble(it, 100.0)?.toFloat() ?: 100f }
                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = col(o.optString("border", "#CCCCCC")); strokeWidth = 0.8f }
                for (r in 0 until rows.length()) {
                    val row = rows.getJSONArray(r)
                    var cx = x
                    for (k in 0 until ncol) {
                        val cy = y + r * rh
                        val header = r == 0 && o.optBoolean("header", true)
                        if (header) c.drawRect(cx, cy, cx + cw[k], cy + rh, Paint().apply { color = col(o.optString("headerFill", "#1F4E79")) })
                        c.drawRect(cx, cy, cx + cw[k], cy + rh, border)
                        val cell = JSONObject().put("size", o.optDouble("size", 11.0)).put("bold", header).put("color", if (header) o.optString("headerColor", "#FFFFFF") else o.optString("color", "#222222"))
                        drawText(c, cell, row.opt(k)?.toString() ?: "", cx + 5, cy + 6, cw[k] - 10)
                        cx += cw[k]
                    }
                }
            }
        }
    }

    private fun drawPage(c: Canvas, page: JSONObject, w: Float, h: Float) {
        c.drawRect(0f, 0f, w, h, Paint().apply { color = col(page.optString("background", "#FFFFFF"), Color.WHITE) })
        val els = page.optJSONArray("elements") ?: return
        for (i in 0 until els.length()) drawElement(c, els.getJSONObject(i), w)
    }

    fun pdf(spec: JSONObject, out: File) {
        val pdf = PdfDocument()
        try {
            val pages = pagesOf(spec)
            for (i in 0 until pages.length()) {
                val pg = pages.getJSONObject(i)
                val w = pg.optDouble("width", spec.optDouble("width", 595.0)).toFloat(); val h = pg.optDouble("height", spec.optDouble("height", 842.0)).toFloat()
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(w.toInt(), h.toInt(), i + 1).create())
                drawPage(page.canvas, pg, w, h)
                pdf.finishPage(page)
            }
            out.outputStream().use { pdf.writeTo(it) }
        } finally { pdf.close() }
    }

    fun png(spec: JSONObject, out: File) {
        val pg = pagesOf(spec).getJSONObject(0)
        val w = pg.optDouble("width", spec.optDouble("width", 595.0)).toFloat(); val h = pg.optDouble("height", spec.optDouble("height", 842.0)).toFloat()
        val s = 2f
        val bmp = Bitmap.createBitmap((w * s).toInt(), (h * s).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.scale(s, s); drawPage(c, pg, w, h)
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    // ---------------------------------------------------------------- Office helpers
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun rtl(s: String) = s.any { it in '\u0590'..'\u08FF' }

    private fun zip(out: File, parts: Map<String, String>) {
        ZipOutputStream(out.outputStream()).use { z -> parts.forEach { (name, text) -> z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray(Charsets.UTF_8)); z.closeEntry() } }
    }

    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    // ---------------------------------------------------------------- DOCX
    fun docx(spec: JSONObject, out: File) {
        val b = StringBuilder()
        fun run(t: String, o: JSONObject?): String {
            val sz = o?.optDouble("size", 0.0) ?: 0.0
            val rpr = (if (o?.optBoolean("bold") == true) "<w:b/>" else "") + (if (o?.optBoolean("italic") == true) "<w:i/>" else "") +
                (if (o != null && o.has("color")) "<w:color w:val=\"${hex6(o.optString("color"))}\"/>" else "") + (if (sz > 0) "<w:sz w:val=\"${(sz * 2).toInt()}\"/>" else "") + (if (rtl(t)) "<w:rtl/>" else "")
            return "<w:r><w:rPr>$rpr</w:rPr><w:t xml:space=\"preserve\">${esc(t)}</w:t></w:r>"
        }
        fun para(t: String, o: JSONObject? = null, style: String? = null, ind: String = ""): String {
            val jc = when (o?.optString("align")) { "center" -> "<w:jc w:val=\"center\"/>"; "right" -> "<w:jc w:val=\"right\"/>"; else -> "" }
            return "<w:p><w:pPr>${if (style != null) "<w:pStyle w:val=\"$style\"/>" else ""}$ind${if (rtl(t)) "<w:bidi/>" else ""}$jc</w:pPr>${run(t, o)}</w:p>"
        }
        spec.optString("title").takeIf { it.isNotBlank() }?.let { b.append(para(it, null, "Title")) }
        val blocks = spec.optJSONArray("blocks") ?: JSONArray()
        for (i in 0 until blocks.length()) {
            val o = blocks.getJSONObject(i)
            when (o.optString("type", "paragraph")) {
                "heading" -> b.append(para(o.optString("text"), null, "Heading" + o.optInt("level", 1).coerceIn(1, 3)))
                "bullets" -> o.optJSONArray("items")?.let { a -> for (k in 0 until a.length()) b.append(para("•  " + a.optString(k), null, null, "<w:ind w:left=\"720\" w:hanging=\"360\"/>")) }
                "numbered" -> o.optJSONArray("items")?.let { a -> for (k in 0 until a.length()) b.append(para("${k + 1}.  " + a.optString(k), null, null, "<w:ind w:left=\"720\" w:hanging=\"360\"/>")) }
                "pagebreak" -> b.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                "table" -> {
                    val rows = o.optJSONArray("rows") ?: JSONArray()
                    b.append("<w:tbl><w:tblPr><w:tblW w:w=\"5000\" w:type=\"pct\"/><w:tblBorders>")
                    for (side in listOf("top", "left", "bottom", "right", "insideH", "insideV")) b.append("<w:$side w:val=\"single\" w:sz=\"4\" w:color=\"999999\"/>")
                    b.append("</w:tblBorders></w:tblPr>")
                    for (r in 0 until rows.length()) {
                        val row = rows.getJSONArray(r); b.append("<w:tr>")
                        for (k in 0 until row.length()) {
                            val header = r == 0 && o.optBoolean("header", true)
                            b.append("<w:tc><w:tcPr>${if (header) "<w:shd w:val=\"clear\" w:fill=\"D9E2F3\"/>" else ""}</w:tcPr>")
                            b.append(para(row.opt(k)?.toString() ?: "", if (header) JSONObject().put("bold", true) else null)).append("</w:tc>")
                        }
                        b.append("</w:tr>")
                    }
                    b.append("</w:tbl><w:p/>")
                }
                else -> b.append(para(o.optString("text"), o))
            }
        }
        val doc = "$XML<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>$b<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/></w:sectPr></w:body></w:document>"
        fun style(id: String, name: String, size: Int, bold: Boolean, color: String) =
            "<w:style w:type=\"paragraph\" w:styleId=\"$id\"><w:name w:val=\"$name\"/><w:basedOn w:val=\"Normal\"/><w:pPr><w:spacing w:before=\"240\" w:after=\"120\"/></w:pPr><w:rPr>${if (bold) "<w:b/>" else ""}<w:color w:val=\"$color\"/><w:sz w:val=\"$size\"/></w:rPr></w:style>"
        val styles = "$XML<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:cs=\"Arial\"/><w:sz w:val=\"22\"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after=\"120\" w:line=\"276\" w:lineRule=\"auto\"/></w:pPr></w:pPrDefault></w:docDefaults>" +
            "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\"><w:name w:val=\"Normal\"/></w:style>" +
            style("Title", "Title", 52, true, "1F3864") + style("Heading1", "heading 1", 36, true, "1F3864") + style("Heading2", "heading 2", 30, true, "2F5496") + style("Heading3", "heading 3", 26, true, "2F5496") + "</w:styles>"
        zip(out, mapOf(
            "[Content_Types].xml" to "$XML<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/><Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/></Types>",
            "_rels/.rels" to "$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"word/document.xml\"/></Relationships>",
            "word/_rels/document.xml.rels" to "$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/styles\" Target=\"styles.xml\"/></Relationships>",
            "word/document.xml" to doc, "word/styles.xml" to styles
        ))
    }

    // ---------------------------------------------------------------- XLSX
    private fun colName(i: Int): String { var n = i; var s = ""; while (n >= 0) { s = ('A' + n % 26) + s; n = n / 26 - 1 }; return s }

    fun xlsx(spec: JSONObject, out: File) {
        val sheets = spec.optJSONArray("sheets") ?: JSONArray().put(spec)
        val parts = LinkedHashMap<String, String>()
        val names = ArrayList<String>()
        for (si in 0 until sheets.length()) {
            val sh = sheets.getJSONObject(si)
            names.add(sh.optString("name", "Sheet${si + 1}").take(31).replace(Regex("[\\\\/?*\\[\\]:]"), "_"))
            val sb = StringBuilder("$XML<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            sh.optJSONArray("columns")?.let { cs -> sb.append("<cols>"); for (k in 0 until cs.length()) sb.append("<col min=\"${k + 1}\" max=\"${k + 1}\" width=\"${cs.optDouble(k, 12.0)}\" customWidth=\"1\"/>"); sb.append("</cols>") }
            sb.append("<sheetData>")
            val rows = sh.optJSONArray("rows") ?: JSONArray()
            for (r in 0 until rows.length()) {
                val row = rows.optJSONArray(r) ?: JSONArray()
                sb.append("<row r=\"${r + 1}\">")
                for (k in 0 until row.length()) {
                    val v = row.opt(k); val ref = colName(k) + (r + 1)
                    val o = v as? JSONObject
                    val value = if (o != null) o.opt("v") else v
                    val styleId = if (r == 0 && sh.optBoolean("header", true)) 2 else if (o?.optBoolean("bold") == true) 1 else 0
                    when {
                        o != null && o.has("f") -> sb.append("<c r=\"$ref\" s=\"$styleId\"><f>${esc(o.optString("f").removePrefix("="))}</f></c>")
                        value is Number -> sb.append("<c r=\"$ref\" s=\"$styleId\"><v>$value</v></c>")
                        value is Boolean -> sb.append("<c r=\"$ref\" s=\"$styleId\" t=\"b\"><v>${if (value) 1 else 0}</v></c>")
                        value == null || value == JSONObject.NULL -> {}
                        else -> sb.append("<c r=\"$ref\" s=\"$styleId\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(value.toString())}</t></is></c>")
                    }
                }
                sb.append("</row>")
            }
            sb.append("</sheetData></worksheet>")
            parts["xl/worksheets/sheet${si + 1}.xml"] = sb.toString()
        }
        val head = hex6(spec.optString("headerFill", "#1F4E79"))
        val ct = StringBuilder("$XML<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        val wb = StringBuilder("$XML<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"$REL\"><sheets>")
        val rel = StringBuilder("$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        names.forEachIndexed { i, n ->
            ct.append("<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
            wb.append("<sheet name=\"${esc(n)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
            rel.append("<Relationship Id=\"rId${i + 1}\" Type=\"$REL/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>")
        }
        rel.append("<Relationship Id=\"rId${names.size + 1}\" Type=\"$REL/styles\" Target=\"styles.xml\"/></Relationships>")
        parts["[Content_Types].xml"] = "$ct</Types>"
        parts["_rels/.rels"] = "$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"
        parts["xl/workbook.xml"] = "$wb</sheets></workbook>"
        parts["xl/_rels/workbook.xml.rels"] = rel.toString()
        parts["xl/styles.xml"] = "$XML<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"3\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF$head\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"3\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/><xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/></cellXfs></styleSheet>"
        zip(out, parts)
    }

    // ---------------------------------------------------------------- PPTX
    fun pptx(spec: JSONObject, out: File) {
        val slides = spec.optJSONArray("slides") ?: JSONArray()
        if (slides.length() == 0) throw IOException("The presentation needs at least one slide")
        val ns = "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"$REL\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\""
        fun emu(v: Double) = (v * 12700).toLong()
        var nextId = 2
        fun shape(o: JSONObject): String {
            val t = o.optString("type", "text")
            val prst = when (t) { "ellipse", "circle" -> "ellipse"; "roundrect" -> "roundRect"; "rect" -> "rect"; else -> "rect" }
            val fill = if (o.has("fill")) "<a:solidFill><a:srgbClr val=\"${hex6(o.optString("fill"))}\"/></a:solidFill>" else "<a:noFill/>"
            val txt = o.optString("text")
            val paras = if (txt.isEmpty()) "<a:p><a:endParaRPr lang=\"en-US\"/></a:p>" else txt.split("\n").joinToString("") { line ->
                "<a:p><a:pPr algn=\"${when (o.optString("align")) { "center" -> "ctr"; "right" -> "r"; else -> "l" }}\"${if (rtl(line)) " rtl=\"1\"" else ""}/><a:r><a:rPr lang=\"en-US\" sz=\"${(o.optDouble("size", 20.0) * 100).toInt()}\" b=\"${if (o.optBoolean("bold")) 1 else 0}\"><a:solidFill><a:srgbClr val=\"${hex6(o.optString("color"), "222222")}\"/></a:solidFill></a:rPr><a:t>${esc(line)}</a:t></a:r></a:p>"
            }
            val id = nextId++
            return "<p:sp><p:nvSpPr><p:cNvPr id=\"$id\" name=\"Shape $id\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x=\"${emu(o.optDouble("x"))}\" y=\"${emu(o.optDouble("y"))}\"/><a:ext cx=\"${emu(o.optDouble("w", 200.0))}\" cy=\"${emu(o.optDouble("h", 60.0))}\"/></a:xfrm><a:prstGeom prst=\"$prst\"><a:avLst/></a:prstGeom>$fill<a:ln><a:noFill/></a:ln></p:spPr><p:txBody><a:bodyPr wrap=\"square\" anchor=\"${if (o.optBoolean("middle")) "ctr" else "t"}\"/><a:lstStyle/>$paras</p:txBody></p:sp>"
        }
        val parts = LinkedHashMap<String, String>()
        val ct = StringBuilder("$XML<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/><Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/><Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/><Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>")
        val ids = StringBuilder(); val prels = StringBuilder("$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>")
        for (i in 0 until slides.length()) {
            val s = slides.getJSONObject(i); nextId = 2
            val body = StringBuilder()
            s.optString("title").takeIf { it.isNotBlank() }?.let { body.append(shape(JSONObject().put("x", 48).put("y", 34).put("w", 864).put("h", 80).put("text", it).put("size", s.optDouble("titleSize", 36.0)).put("bold", true).put("color", s.optString("titleColor", "#1F3864")).put("middle", true))) }
            s.optString("subtitle").takeIf { it.isNotBlank() }?.let { body.append(shape(JSONObject().put("x", 48).put("y", 118).put("w", 864).put("h", 40).put("text", it).put("size", 20).put("color", "#666666"))) }
            s.optJSONArray("bullets")?.let { b -> body.append(shape(JSONObject().put("x", 60).put("y", 170).put("w", 840).put("h", 340).put("size", s.optDouble("bulletSize", 22.0)).put("color", s.optString("color", "#222222")).put("text", (0 until b.length()).joinToString("\n") { "•  " + b.optString(it) }))) }
            s.optJSONArray("shapes")?.let { sh -> for (k in 0 until sh.length()) body.append(shape(sh.getJSONObject(k))) }
            val bg = if (s.has("background")) "<p:bg><p:bgPr><a:solidFill><a:srgbClr val=\"${hex6(s.optString("background"), "FFFFFF")}\"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>" else ""
            parts["ppt/slides/slide${i + 1}.xml"] = "$XML<p:sld $ns><p:cSld>$bg<p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/>$body</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>"
            parts["ppt/slides/_rels/slide${i + 1}.xml.rels"] = "$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/></Relationships>"
            ct.append("<Override PartName=\"/ppt/slides/slide${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>")
            ids.append("<p:sldId id=\"${256 + i}\" r:id=\"rId${i + 2}\"/>")
            prels.append("<Relationship Id=\"rId${i + 2}\" Type=\"$REL/slide\" Target=\"slides/slide${i + 1}.xml\"/>")
        }
        val tree = "<p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree>"
        val clr = "<a:clrScheme name=\"Office\"><a:dk1><a:srgbClr val=\"000000\"/></a:dk1><a:lt1><a:srgbClr val=\"FFFFFF\"/></a:lt1><a:dk2><a:srgbClr val=\"1F497D\"/></a:dk2><a:lt2><a:srgbClr val=\"EEECE1\"/></a:lt2><a:accent1><a:srgbClr val=\"4F81BD\"/></a:accent1><a:accent2><a:srgbClr val=\"C0504D\"/></a:accent2><a:accent3><a:srgbClr val=\"9BBB59\"/></a:accent3><a:accent4><a:srgbClr val=\"8064A2\"/></a:accent4><a:accent5><a:srgbClr val=\"4BACC6\"/></a:accent5><a:accent6><a:srgbClr val=\"F79646\"/></a:accent6><a:hlink><a:srgbClr val=\"0000FF\"/></a:hlink><a:folHlink><a:srgbClr val=\"800080\"/></a:folHlink></a:clrScheme>"
        val sf = "<a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>"
        val ln = "<a:ln w=\"9525\">$sf</a:ln>"
        val theme = "$XML<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" name=\"Office\"><a:themeElements>$clr<a:fontScheme name=\"Office\"><a:majorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:majorFont><a:minorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:minorFont></a:fontScheme><a:fmtScheme name=\"Office\"><a:fillStyleLst>$sf$sf$sf</a:fillStyleLst><a:lnStyleLst>$ln$ln$ln</a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst>$sf$sf$sf</a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>"
        parts["[Content_Types].xml"] = "$ct</Types>"
        parts["_rels/.rels"] = "$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"ppt/presentation.xml\"/></Relationships>"
        parts["ppt/presentation.xml"] = "$XML<p:presentation $ns><p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/></p:sldMasterIdLst><p:sldIdLst>$ids</p:sldIdLst><p:sldSz cx=\"12192000\" cy=\"6858000\"/><p:notesSz cx=\"6858000\" cy=\"9144000\"/></p:presentation>"
        parts["ppt/_rels/presentation.xml.rels"] = "$prels</Relationships>"
        parts["ppt/slideMasters/slideMaster1.xml"] = "$XML<p:sldMaster $ns><p:cSld><p:bg><p:bgRef idx=\"1001\"><a:schemeClr val=\"bg1\"/></p:bgRef></p:bg>$tree</p:cSld><p:clrMap bg1=\"lt1\" tx1=\"dk1\" bg2=\"lt2\" tx2=\"dk2\" accent1=\"accent1\" accent2=\"accent2\" accent3=\"accent3\" accent4=\"accent4\" accent5=\"accent5\" accent6=\"accent6\" hlink=\"hlink\" folHlink=\"folHlink\"/><p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/></p:sldLayoutIdLst></p:sldMaster>"
        parts["ppt/slideMasters/_rels/slideMaster1.xml.rels"] = "$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/><Relationship Id=\"rId2\" Type=\"$REL/theme\" Target=\"../theme/theme1.xml\"/></Relationships>"
        parts["ppt/slideLayouts/slideLayout1.xml"] = "$XML<p:sldLayout $ns type=\"blank\"><p:cSld name=\"Blank\">$tree</p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>"
        parts["ppt/slideLayouts/_rels/slideLayout1.xml.rels"] = "$XML<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$REL/slideMaster\" Target=\"../slideMasters/slideMaster1.xml\"/></Relationships>"
        parts["ppt/theme/theme1.xml"] = theme
        zip(out, parts)
    }

    /** Builds the file for [kind]: pdf, png, docx, xlsx or pptx. */
    fun make(kind: String, spec: JSONObject, out: File) {
        out.parentFile?.mkdirs()
        when (kind.lowercase()) {
            "pdf" -> pdf(spec, out); "png" -> png(spec, out); "docx" -> docx(spec, out); "xlsx" -> xlsx(spec, out); "pptx" -> pptx(spec, out)
            else -> throw IOException("Unknown kind \"$kind\". Use pdf, png, docx, xlsx or pptx (text formats like csv, md, html, svg, json are written with write_file).")
        }
    }
}
