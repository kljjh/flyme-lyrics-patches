package io.github.kljjh.flymelyrics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.test.*

/** Tests the UNMODIFIED official release DEX, not a copy of its Java source. */
class OfficialIntegrationTest {
    private fun officialClasses(): List<ClassDef> {
        val file = File(System.getProperty("officialBundle"))
        assertTrue(file.isFile, "Download official patches-2.1.0.mpp first")
        assertEquals("114eafa139ea3018f35af3498e8e67fe7850d4dd22a6065a02e891699dcc5e96",
            MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) })
        return ZipFile(file).use { zip ->
            zip.entries().asSequence().filter { it.name.endsWith(".mpe") }.flatMap { entry ->
                DexBackedDexFile(Opcodes.getDefault(), zip.getInputStream(entry).readBytes()).classes.asSequence()
            }.toList()
        }
    }

    @Test fun hooksActualOfficialDexAndRoundTrips() {
        val all = officialClasses()
        assertTrue(all.single { it.type == OfficialHooks.UTILS }.methods.any { it.name == "getContext" && it.parameterTypes.isEmpty() })
        val cls = MutableClass(all.single { it.type == OfficialHooks.LOCKSCREEN })
        val originalCount = cls.methods.size
        OfficialHooks.instrument(cls)
        assertEquals(originalCount + 4, cls.methods.size)
        val calls = cls.methods.flatMap { m -> m.implementation?.instructions?.mapNotNull { (it as? ReferenceInstruction)?.reference as? MethodReference }.orEmpty() }
        assertTrue(calls.any { it.definingClass == OfficialHooks.BRIDGE && it.name == "onLine" })
        assertTrue(calls.any { it.definingClass == OfficialHooks.BRIDGE && it.name == "onPlaybackState" })
        val store = MemoryDataStore()
        DexPool.writeTo(store, ImmutableDexFile(Opcodes.getDefault(), listOf(cls)))
        val rewritten = DexBackedDexFile(Opcodes.getDefault(), store.data).classes.single()
        assertEquals(cls.methods.size, rewritten.methods.count())
        val output = File("build/test-results/official-lockscreen-patched.dex")
        output.parentFile.mkdirs()
        output.writeBytes(store.data)
        // DEX is also checked with Android dexdump by the CI workflow.
    }

    @Test fun rejectsAlreadyInstrumentedOrChangedOfficialCode() {
        val original = officialClasses().single { it.type == OfficialHooks.LOCKSCREEN }
        val cls = MutableClass(original)
        OfficialHooks.instrument(cls)
        assertFailsWith<PatchException> { OfficialHooks.instrument(cls) }
        val changed = MutableClass(original)
        changed.methods.removeAll { it.name == "getCurrentLine" }
        val count = changed.methods.size
        assertFailsWith<PatchException> { OfficialHooks.instrument(changed) }
        assertEquals(count, changed.methods.size, "Validation must finish before adding helpers")
    }

    @Test fun requiresHostHooksRatherThanOnlyBundledUnusedClasses() {
        val host = MutableClass(ImmutableClassDef("Lfixture/Player;", AccessFlags.PUBLIC.value,
            "Ljava/lang/Object;", emptyList(), null, emptySet(), emptyList(), emptyList()))
        val method = ImmutableMethod(host.type, "dispatch", emptyList(), "V", AccessFlags.STATIC.value,
            emptySet(), null, MutableMethodImplementation(2)).toMutable().apply {
            addInstructions(0, """
                const/4 v0, 0x0
                const/4 v1, 0x0
                invoke-static {v0}, ${OfficialHooks.PANEL_INSTALLER}->onTabSelected(Landroid/view/ViewGroup;)V
                return-void
            """)
        }
        host.methods.add(method)
        assertTrue(OfficialHooks.hostHooks(host))
        OfficialHooks.requireSelection(true)
        host.methods.clear()
        assertFalse(OfficialHooks.hostHooks(host))
        assertTrue(assertFailsWith<PatchException> { OfficialHooks.requireSelection(false) }.message!!.contains("official Morphe source"))
        assertTrue(flymeLyricsPatch.dependencies.isEmpty(), "Must not embed or link to a second copy of official patches")
    }
}
