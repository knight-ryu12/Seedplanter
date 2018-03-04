package faith.elguadia.seedplanter;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Signing {
    private String tmpDirStr;

    Signing() throws IOException {
        if (!SystemUtils.IS_OS_WINDOWS || System.getenv("ProgramFiles(x86)") == null)
            throw new IOException("You are not running Windows 64 bit!");

        tmpDirStr = Files.createTempDirectory("Seedplanter").toString();
    }

    public void ExportFiles(byte[] footer, Path ctcert) throws IOException {
        Files.copy(getClass().getClassLoader().getResourceAsStream("ctr-dsiwaretool.exe"), Paths.get(tmpDirStr, "ctr-dsiwaretool.exe"));
        Files.copy(getClass().getClassLoader().getResourceAsStream("libcrypto-1_1-x64__.dll"), Paths.get(tmpDirStr, "libcrypto-1_1-x64__.dll"));
        Files.copy(getClass().getClassLoader().getResourceAsStream("zlib1__.dll"), Paths.get(tmpDirStr, "zlib1__.dll"));

        Files.write(Paths.get(tmpDirStr, "footer.bin"), footer, StandardOpenOption.CREATE);
        Files.copy(ctcert, Paths.get(tmpDirStr, "ctcert.bin"));
    }

    public void DoSigning() throws IOException, InterruptedException {
        String[] command = {
                tmpDirStr + "\\ctr-dsiwaretool.exe", tmpDirStr + "\\footer.bin", tmpDirStr + "\\ctcert.bin" , "--write"
        };

        System.out.println("Executing command --> " + String.join(" ", command));
        System.out.println("============== EXECUTING CTR-DSIWARETOOL ==============");

        ProcessBuilder procb = new ProcessBuilder(command);
        procb.inheritIO();
        Process proc = procb.start();

        proc.waitFor();
        int exitValue = proc.waitFor();

        System.out.println("============== END OF CTR-DSIWARETOOL ==============");

        if (exitValue == 0)
            System.out.println("======== Successfully executed the command ========");
        else {
            System.out.println("======== Failed to execute the command ========");
            throw new IOException("Failed to sign the footer!");
        }
    }

    public byte[] ImportFooter() throws IOException {
        return Files.readAllBytes(Paths.get(tmpDirStr, "footer.bin"));
    }
}