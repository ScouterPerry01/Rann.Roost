// File path and name: app/desktop/src/test/kotlin/ca/schippers/hfm/desktop/LoopbackPipeTest.kt
// Modified On Timestamp: 2026-10-20 @ 10:05 EDT
// Created On Timestamp: 2026-10-20 @ 10:05 EDT
// File Description: Tests of when Java's wake-up pipe is switched to a TCP connection, and that a
//   network client still works with the switch.
// Uses: LoopbackPipe, a separate Java process.
// Used By: ./gradlew :app:desktop:test.
// Purpose: The Microsoft Store's third rejection must not come back (MSIX redirects socket files).

package ca.schippers.hfm.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoopbackPipeTest {

    @Test
    fun `on Windows the socket-file folder is one that cannot exist inside the runtime`() {
        assertEquals(
            Path.of("C:\\Program Files\\WindowsApps\\RANN\\runtime", "no-socket-files").toString(),
            LoopbackPipe.folder("Windows 11", "C:\\Program Files\\WindowsApps\\RANN\\runtime", null),
        )
    }

    @Test
    fun `elsewhere, or when already given, Java is left as it is`() {
        assertNull(LoopbackPipe.folder("Linux", "/opt/ranns-roost/lib/runtime", null))
        assertNull(LoopbackPipe.folder("Windows 11", "C:\\runtime", "D:\\sockets"))
        assertNull(LoopbackPipe.folder("Windows 11", null, null))
    }

    /** In a fresh Java (the setting is read once per process): with the switch, a selector and an HttpClient still open. */
    @Test
    fun `a network client still opens with the switch`() {
        val home = System.getProperty("java.home")
        val folder = LoopbackPipe.folder(System.getProperty("os.name"), home, null) ?: return
        val source = Files.createTempDirectory("loopback-pipe").resolve("Probe.java")
        Files.writeString(
            source,
            """
            public class Probe {
                public static void main(String[] a) throws Exception {
                    java.nio.channels.Selector.open().close();
                    java.net.http.HttpClient.newHttpClient();
                    System.out.println("ok");
                }
            }
            """.trimIndent(),
        )
        val java = Path.of(home, "bin", "java").toString()
        val process = ProcessBuilder(java, "-Djdk.net.unixdomain.tmpdir=$folder", source.toString()).redirectErrorStream(true).start()
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "the probe did not finish")
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.exitValue(), output)
        assertEquals("ok", output.trim())
    }
}
