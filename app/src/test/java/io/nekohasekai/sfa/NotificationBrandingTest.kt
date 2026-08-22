package io.nekohasekai.sfa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class NotificationBrandingTest {
    @Test
    fun `notification icon is a 24dp monochrome vector`() {
        val vector = parseXml(appDir.resolve("src/main/res/drawable/$ICON.xml"))
        val root = vector.documentElement

        assertEquals("vector", root.tagName)
        assertEquals("24dp", root.androidAttribute("width"))
        assertEquals("24dp", root.androidAttribute("height"))
        assertEquals("24", root.androidAttribute("viewportWidth"))
        assertEquals("24", root.androidAttribute("viewportHeight"))

        val paths = root.getElementsByTagName("path")
        assertTrue("Notification icon must contain at least one path", paths.length > 0)
        for (index in 0 until paths.length) {
            val path = paths.item(index) as Element
            assertEquals("#FFFFFFFF", path.androidAttribute("fillColor"))
            assertTrue(path.androidAttribute("pathData").isNotBlank())
        }
    }

    @Test
    fun `all notification builders use the branded small icon`() {
        val smallIconPattern = Regex("""setSmallIcon\s*\(\s*R\.drawable\.([A-Za-z0-9_]+)\s*\)""")
        val smallIcons = kotlinSources().flatMap { source ->
            smallIconPattern.findAll(String(Files.readAllBytes(source), Charsets.UTF_8)).map { match ->
                source to match.groupValues[1]
            }.toList()
        }

        assertTrue("Expected at least one notification small icon", smallIcons.isNotEmpty())
        smallIcons.forEach { (source, resource) ->
            assertEquals("Unexpected small icon in $source", ICON, resource)
        }
    }

    @Test
    fun `quick settings tile uses the branded icon`() {
        val manifest = parseXml(appDir.resolve("src/main/AndroidManifest.xml"))
        val services = manifest.getElementsByTagName("service")
        val tileService = (0 until services.length)
            .map { services.item(it) as Element }
            .firstOrNull { it.androidAttribute("name") == ".bg.TileService" }

        assertEquals("@drawable/$ICON", tileService?.androidAttribute("icon"))
    }

    private fun kotlinSources(): List<Path> = Files.walk(appDir.resolve("src")).use { paths ->
        paths.iterator().asSequence()
            .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
            .toList()
    }

    private fun parseXml(path: Path): Document = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(path.toFile())

    private fun Element.androidAttribute(name: String): String = getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val ICON = "ic_goolvpn_notification"

        val appDir: Path = Path.of(System.getProperty("user.dir")).let { workingDir ->
            listOf(workingDir, workingDir.resolve("app"))
                .first { Files.isDirectory(it.resolve("src/main")) }
        }
    }
}
