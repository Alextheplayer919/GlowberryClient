package org.chimeramc.client.core.minecraft

import java.io.File
import java.io.RandomAccessFile

/**
 * Minimal ELF header reader used to validate native libraries before they are handed to
 * dlopen (item G3).
 *
 * The loader's own failure mode for a wrong-architecture library is a raw
 * `UnsatisfiedLinkError: dlopen failed ... is 32-bit instead of 64-bit`, which does not say
 * which file it was or what was wrong. Reading the ELF header lets the launcher name the
 * offending library and its actual architecture before a launch attempt, and refuse to load
 * a library that cannot work in this process.
 *
 * Pure JVM (no Android runtime), so it can be unit-tested.
 */
object ElfHeaderValidator {

    /** File starts with the ELF magic (`\x7FELF`). */
    private const val ELF_MAGIC_0 = 0x7F
    private const val ELF_MAGIC_1 = 'E'.code
    private const val ELF_MAGIC_2 = 'L'.code
    private const val ELF_MAGIC_3 = 'F'.code

    private const val EI_CLASS = 4
    private const val EI_DATA = 5

    /** EI_CLASS values. */
    private const val ELFCLASS32 = 1
    private const val ELFCLASS64 = 2

    /** Endianness values (EI_DATA). */
    private const val ELFDATA2LSB = 1
    private const val ELFDATA2MSB = 2

    /** e_machine values for the ABIs Android can run. */
    const val EM_ARM = 40
    const val EM_X86 = 3
    const val EM_AARCH64 = 183
    const val EM_X86_64 = 62

    private const val MAGIC_BYTES = 4
    private const val HEADER_MIN_LENGTH = 20

    /** A parsed, valid ELF header. */
    data class ElfHeader(
        val is64Bit: Boolean,
        val machine: Int
    ) {
        /** Canonical Android ABI name for this header, or null when unrecognised. */
        val abi: String?
            get() = when (machine) {
                EM_AARCH64 -> AbiBitness.ABI_ARM64
                EM_ARM -> AbiBitness.ABI_ARM32
                EM_X86_64 -> "x86_64"
                EM_X86 -> "x86"
                else -> null
            }
    }

    /** True when [file] starts with a valid, recognised ELF header. */
    @JvmStatic
    fun isElf(file: File): Boolean = read(file) != null

    /**
     * Reads the ELF class + e_machine from [file], or null when the file is not a parseable
     * ELF (too short, bad magic, unsupported endianness).
     */
    @JvmStatic
    fun read(file: File): ElfHeader? {
        if (file == null || !file.isFile) return null
        val length = file.length()
        if (length < HEADER_MIN_LENGTH) return null

        try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(0L)
                val magic = ByteArray(MAGIC_BYTES)
                raf.readFully(magic)
                if (magic[0] != ELF_MAGIC_0.toByte()
                    || magic[1] != ELF_MAGIC_1.toByte()
                    || magic[2] != ELF_MAGIC_2.toByte()
                    || magic[3] != ELF_MAGIC_3.toByte()
                ) {
                    return null
                }

                val elfClass = raf.readUnsignedByte()
                val data = raf.readUnsignedByte()
                if (elfClass != ELFCLASS32 && elfClass != ELFCLASS64) return null
                if (data != ELFDATA2LSB && data != ELFDATA2MSB) return null

                // e_machine sits at offset 18; byte order depends on EI_DATA.
                raf.seek(18L)
                val machine = if (data == ELFDATA2LSB) {
                    raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8)
                } else {
                    (raf.readUnsignedByte() shl 8) or raf.readUnsignedByte()
                }

                return ElfHeader(is64Bit = elfClass == ELFCLASS64, machine = machine)
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * True when [file] is a native library this process can load: it is a recognised ELF and a
     * 64-bit header when [processIs64Bit] is true, a 32-bit header otherwise.
     *
     * Files that are not ELF at all (an empty stub, a Java archive) are reported compatible so
     * callers that do not require ELF do not break; use [requireUsableLibrary] to be strict.
     */
    @JvmStatic
    fun isProcessCompatible(file: File, processIs64Bit: Boolean): Boolean {
        val header = read(file) ?: return true
        return header.is64Bit == processIs64Bit
    }

    /** A descriptive, one-line reason when [file] is a recognised ELF that the process cannot load. */
    @JvmStatic
    fun describeMismatch(file: File, processIs64Bit: Boolean): String? {
        val header = read(file) ?: return null
        if (header.is64Bit == processIs64Bit) return null
        val headerAbi = header.abi
        val processAbi = if (processIs64Bit) AbiBitness.ABI_ARM64 else AbiBitness.ABI_ARM32
        val expected = if (processIs64Bit) "64-bit" else "32-bit"
        val actual = if (header.is64Bit) "64-bit" else "32-bit"
        val name = headerAbi ?: "machine ${header.machine}"
        return "$file: is $actual ($name), but the process runs $expected ($processAbi) — this library cannot load here"
    }
}