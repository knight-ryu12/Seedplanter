package faith.elguadia.seedplanter;

import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Signing {
    private String tmpDirStr;

    Signing() throws IOException {
        tmpDirStr = Files.createTempDirectory("Seedplanter").toString();
    }

    public void ExportFiles(byte[] footer, Path ctcert) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            Files.copy(getClass().getClassLoader().getResourceAsStream("ctr-dsiwaretool.exe"), Paths.get(tmpDirStr, "ctr-dsiwaretool.exe"));
            Files.copy(getClass().getClassLoader().getResourceAsStream("libcrypto-1_1-x64__.dll"), Paths.get(tmpDirStr, "libcrypto-1_1-x64__.dll"));
            Files.copy(getClass().getClassLoader().getResourceAsStream("zlib1__.dll"), Paths.get(tmpDirStr, "zlib1__.dll"));
        } else if (SystemUtils.IS_OS_MAC) {
            Files.copy(getClass().getClassLoader().getResourceAsStream("mac_ctr-dsiwaretool"), Paths.get(tmpDirStr, "mac_ctr-dsiwaretool"));
        } else if (SystemUtils.IS_OS_LINUX) {
            Files.copy(getClass().getClassLoader().getResourceAsStream("linux_ctr-dsiwaretool"), Paths.get(tmpDirStr, "linux_ctr-dsiwaretool"));
        }

        Files.write(Paths.get(tmpDirStr, "footer.bin"), footer, StandardOpenOption.CREATE);
        Files.copy(ctcert, Paths.get(tmpDirStr, "ctcert.bin"));
    }

    public void DoSigning() throws IOException, InterruptedException {
        String command = null;

        if (SystemUtils.IS_OS_WINDOWS) {
            System.out.println("============== DETECTED WINDOWS ==============");
            command = tmpDirStr + "\\ctr-dsiwaretool.exe " + tmpDirStr + "\\footer.bin " + tmpDirStr + "\\ctcert.bin " + "--write";
        } else if (SystemUtils.IS_OS_MAC) {
            System.out.println("============== DETECTED MAC ==============");
            command = "." + tmpDirStr + "/mac_ctr-dsiwaretool "   + tmpDirStr + "/footer.bin " + tmpDirStr + "/ctcert.bin " + "--write";
        } else if (SystemUtils.IS_OS_LINUX) {
            System.out.println("============== DETECTED LINUX ==============");
            command = "." + tmpDirStr + "/linux_ctr-dsiwaretool " + tmpDirStr + "/footer.bin " + tmpDirStr + "/ctcert.bin " + "--write";
        }

        System.out.println("============== EXECUTING CTR-DSIWARETOOL ==============");
        System.out.println("Executing command --> " + command);
        Process proc = Runtime.getRuntime().exec(command);

        proc.waitFor();
        final int exitValue = proc.waitFor();

        System.out.println("============== OUTPUT FROM CTR-DSIWARETOOL ==============");
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String str;
        while ((str = stdInput.readLine()) != null)
            System.out.println(str);
        // read any errors from the attempted command
        while ((str = stdError.readLine()) != null)
            System.out.println(str);
        System.out.println("============== END OF CTR-DSIWARETOOL ==============");

        if (exitValue == 0)
            System.out.println("Successfully executed the command");
        else {
            System.out.println("Failed to execute the command");
            throw new IOException("Failed to sign the footer!");
        }
    }

    public byte[] ImportFooter() throws IOException {
        return Files.readAllBytes(Paths.get(tmpDirStr, "footer.bin"));
    }
}