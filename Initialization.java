import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

class Initialization {

    private static String currentDir;
    private static String permissionsOutput;
    private static String hash;
    private static String outputFilePath;
    private static String reportFilePath;

    // including monitoring_directory, make it 0 if only No of Sub Directories are needed
    private static Integer totalNoOfDirParsed = 1;

    private static Integer totalNoOfFilesParsed = 0;
    private static Long startTimeSeconds;

    // java Verification.java <current_directory> <ls -lR output> <hash> <output_file> <report_file> <start_time>
    public static void main(String[] args) {
        currentDir = args[0];
        permissionsOutput = args[1];
        if (args[2].equalsIgnoreCase("md5")) {
            hash = args[2].toUpperCase();
        }
        if (args[2].equalsIgnoreCase("sha1")) {
            hash = "SHA-1";
        }
        outputFilePath = args[3];
        reportFilePath = args[4];
        startTimeSeconds = Long.parseLong(args[5]);
        parse();
        Long endTimeSeconds = System.currentTimeMillis() / 1000;
        writeReport(endTimeSeconds - startTimeSeconds);
    }

    private static void writeReport(Long timetaken) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFilePath, true))) {
            writer.append("Monitored Directory: " + currentDir);
            writer.newLine();
            writer.append("Verification File: " + outputFilePath);
            writer.newLine();
            writer.append("No of Directories Parsed: " + totalNoOfDirParsed);
            writer.newLine();
            writer.append("No of Files Parsed: " + totalNoOfFilesParsed);
            writer.newLine();
            writer.append("Time taken: " + timetaken + " sec");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parse() {
        Integer index = 2;
        String[] output = permissionsOutput.split("\n");
        Integer length = output.length;
        String dir = currentDir;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            for (index = 2; index < length; index++) {
                if (output[index] == "") {
                    dir = getDirName(output[index + 1]);
                    index += 3;
                }
                writer.append(parseLine(output[index], dir));
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getDirName(String line) {
        return line.replace(":", "").replace(".", currentDir);
    }

    private static String parseLine(String line, String dir) {
        // Required format: Path,Size,User,Group,Access_Rights,Modification_Date,Hash
        // line format: permissions(0) symbolic_links(1) user(2) group(3) size(4)
        // month(5) date(6) time(7) file/dirname(8)
        List<String> row = new ArrayList<>();
        String[] output = line.trim().replaceAll(" +", " ").split(" ");
        String filePath = getFilePath(dir, output[8]);
        File file = new File(filePath);
        row.add(filePath);
        row.add(output[4]);
        row.add(output[2]);
        row.add(output[3]);
        row.add(output[0]);
        row.add(getModificationTimestamp(file.lastModified()));
        if (output[0].charAt(0) == 'd') {
            totalNoOfDirParsed++;
            row.add("N/A");
        } else {
            totalNoOfFilesParsed++;
            row.add(getHash(file));
        }
        return String.join(",", row);
    }

    private static String getFilePath(String dir, String fileName) {
        return dir + "/" + fileName;
    }

    private static String getModificationTimestamp(Long lastModified) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(lastModified);
    }

    private static String getHash(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance(hash);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[2048];
                int bytesCount = 0;
                while ((bytesCount = fis.read(byteArray)) != -1) {
                    md.update(byteArray, 0, bytesCount);
                }
                byte[] digest = md.digest();
                // convert to hexadecimal string
                for (int i = 0; i < digest.length; i++) {
                    sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}