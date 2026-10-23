package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Pins the ELF header parsing and ABI-mismatch detection (item G3).
 *
 * The validator reads only the first 20 bytes of the ELF header, so the test writes synthetic
 * headers: enough of the e_ident/e_machine that a real object file would carry. The critical
 * properties — class (32/64), endianness, machine — are all in that window.
 */
public class ElfHeaderValidatorTest {

    private static File temp(byte[] content, String name) throws IOException {
        File file = File.createTempFile(name, ".so");
        file.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content);
        }
        return file;
    }

    private static byte[] elf(int elfClass, int data, int machine) {
        byte[] b = new byte[20];
        b[0] = 0x7f;
        b[1] = 'E';
        b[2] = 'L';
        b[3] = 'F';
        b[4] = (byte) elfClass;
        b[5] = (byte) data;
        if (data == 1) {
            // little-endian e_machine at offset 18
            b[18] = (byte) (machine & 0xff);
            b[19] = (byte) ((machine >> 8) & 0xff);
        } else {
            b[18] = (byte) ((machine >> 8) & 0xff);
            b[19] = (byte) (machine & 0xff);
        }
        return b;
    }

    private static final byte[] ARM64_LE = elf(2, 1, ElfHeaderValidator.EM_AARCH64);
    private static final byte[] ARM32_LE = elf(1, 1, ElfHeaderValidator.EM_ARM);
    private static final byte[] X86_64_LE = elf(2, 1, ElfHeaderValidator.EM_X86_64);

    @Test
    public void readsArm64Header() throws IOException {
        ElfHeaderValidator.ElfHeader header = ElfHeaderValidator.read(temp(ARM64_LE, "libarm64"));
        assertNotNull(header);
        assertTrue(header.is64Bit());
        assertEquals(AbiBitness.ABI_ARM64, header.getAbi());
    }

    @Test
    public void readsArm32Header() throws IOException {
        ElfHeaderValidator.ElfHeader header = ElfHeaderValidator.read(temp(ARM32_LE, "libarm32"));
        assertNotNull(header);
        assertFalse(header.is64Bit());
        assertEquals(AbiBitness.ABI_ARM32, header.getAbi());
    }

    @Test
    public void arm64LoadsIn64BitProcess() throws IOException {
        assertTrue(ElfHeaderValidator.isProcessCompatible(temp(ARM64_LE, "libarm64"), true));
    }

    @Test
    public void arm32RefusedIn64BitProcess() throws IOException {
        File file = temp(ARM32_LE, "libarm32");
        assertFalse(ElfHeaderValidator.isProcessCompatible(file, true));
        String message = ElfHeaderValidator.describeMismatch(file, true);
        assertNotNull(message);
        assertTrue(message.contains("32-bit"));
        assertTrue(message.contains(file.getAbsolutePath()));
    }

    @Test
    public void arm64RefusedIn32BitProcess() throws IOException {
        File file = temp(ARM64_LE, "libarm64");
        assertFalse(ElfHeaderValidator.isProcessCompatible(file, false));
        assertNotNull(ElfHeaderValidator.describeMismatch(file, false));
    }

    @Test
    public void x8664LoadsIn64BitProcess() throws IOException {
        assertTrue(ElfHeaderValidator.isProcessCompatible(temp(X86_64_LE, "libx64"), true));
    }

    @Test
    public void notAnElfIsReportedCompatible() throws IOException {
        File file = temp(new byte[]{-1, 'A', 'B', 'C', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, "libjunk");
        assertNull(ElfHeaderValidator.read(file));
        assertTrue(ElfHeaderValidator.isProcessCompatible(file, true));
    }

    @Test
    public void shortFileIsNotElf() throws IOException {
        File file = temp(new byte[8], "libshort");
        assertNull(ElfHeaderValidator.read(file));
    }
}