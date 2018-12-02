import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

@SuppressWarnings("Duplicates")
public class Main {

    private static Disk diskObject;
    //19641
    private static Scanner kb;
    private static int allocationType;
    private static int blockSize = 512;

    public static void main(String[] args) {
        diskObject = new Disk();
        System.out.println("Welcome to Disk Simulation");
        kb = new Scanner(System.in);
        allocationType = printAllocationMenu();
        initBitmap();
        initFileAllocTable();
        System.out.println();
        mainMenu();
        System.out.println("Simulation Ended");
    }

    public static void initFileAllocTable() {
        StringBuilder empty = new StringBuilder();
        for (int i = 0; i < blockSize; i++) {
            empty.append("0");
        }
        writeToDisk(empty.toString().getBytes(), 0);
    }


    public static void mainMenu() {
        System.out.println("1) Display a file\n" +
                "2) Display the file table\n" +
                "3) Display the free space bitmap\n" +
                "4) Display a disk block\n" +
                "5) Copy a file from the simulation to a file on the real system\n" +
                "6) Copy a file from the real system to a file in the simulation\n" +
                "7) Delete a file\n" +
                "8) Exit");
        while (true) {
            System.out.print("Enter Choice : ");
            String input = kb.nextLine();
            if (input.equals("1")) {
                getSimulationFile(true);
            } else if (input.equals("2")) {
                String fileAllocationTable = new String(diskObject.read(0));
                System.out.println("-------- FILE ALLOCATION TABLE -------");
                System.out.println(fileAllocationTable);

            } else if (input.equals("3")) {
                byte[] bitmap = diskObject.read(1);
                for (int i = 0; i < bitmap.length; i++) {
                    if (i % 32 == 0) {
                        System.out.println();
                    }
                    System.out.print(bitmap[i]);
                }

            } else if (input.equals("4")) {
                System.out.print("Enter disk block to display 0-255: ");
                String in = kb.nextLine();
                try {
                    int val = Integer.parseInt(in);
                    if (val < 0 || val > 255) {
                        System.out.println("Error: Enter number 0-255, please try again.");
                    } else {
                        String ans = new String(diskObject.read(val));
                        System.out.println("--------- Disk Block at " + val + " --------");
                        System.out.println(ans);
                    }
                } catch (Exception ignored) {
                    System.out.println("Error: Enter number 0-255, please try again.");
                }
            } else if (input.equals("5")) {
                getSimulationFile(false);
            } else if (input.equals("6")) {
                String[] files = displayFilesInFolder();
                if (files != null) {
                    startRealFileToSimulationCopy(files);
                }

            } else if (input.equals("7")) {
                deleteFile();
            } else if (input.equals("8")) {
                return;
            }
            System.out.println();
        }
    }

    public static void deleteFile() {
        System.out.println("------------- FILES IN SYSTEM ------------");
        String[] filesInSystem = readFileAllocationTable();
        int length = 0;
        for (String s : filesInSystem) {
            if (s != null) {
                if (s.contains("File")) {
                    String[] e = s.split(" ");
                    System.out.println(e[1]);
                    length++;
                }
            }
        }
        if (length <= 0) {
            System.out.println("No Files stored in System. Please try again later");
            return;
        }
        System.out.print("Choose file to delete: ");
        String fileInput = kb.nextLine().strip();
        boolean fileFound = false;
        String answer = "";
        for (String f : filesInSystem) {
            if (f != null && f.contains(fileInput)) {
                fileFound = true;
                answer = f;
                break;
            }
        }
        if (!fileFound) {
            System.out.println("File not found. Please try again");
        } else {
            StringBuilder tableBuilder = new StringBuilder();
            for (String s : filesInSystem) {
                if (s != null && s.contains("File") && !s.contains(fileInput)) {
                    tableBuilder.append(s).append(",");
                }
            }
            StringBuilder table = new StringBuilder(tableBuilder.toString());
            while (table.length() < blockSize) {
                table.append("0");
            }
            writeToDisk(table.toString().getBytes(), 0);
            deleteAllocation(answer);
        }
    }

    public static void deleteAllocation(String file) {
        String[] input = file.split(" ");
        byte[] bitmap = diskObject.read(1);
        if (allocationType == 1) {
            int start = Integer.parseInt(input[2]);
            int length = Integer.parseInt(input[3]);
            for (int i = start; i < start + length; i++) {
                bitmap[i] = 0;
            }
        } else if (allocationType == 2) {
            int start = Integer.parseInt(input[2]);
            int length = Integer.parseInt(input[3]);
            for (int i = 0; i < length; i++) {
                byte[] data = diskObject.read(start);
                bitmap[start] = 0;
                start = data[data.length - 1] & 255;
            }
        } else if (allocationType == 3) {
            int start = Integer.parseInt(input[2]);
            byte[] index = diskObject.read(start);
            while (start != 0) {
                bitmap[start] = 0;
                start = index[0] & 255;
            }
        }

        writeToDisk(bitmap, 1);
    }

    public static void initBitmap() {
        byte[] bitmap = new byte[blockSize];
        for (int i = 0; i < bitmap.length; i++) {
            bitmap[i] = 0;
        }
        bitmap[1] = 1;
        diskObject.write(bitmap, 1);
    }

    public static void startRealFileToSimulationCopy(String[] files) {
        System.out.print("Copy from: ");
        String fileInput = kb.nextLine().strip();
        boolean fileFound = false;
        for (String f : files) {
            if (fileInput.equals(f)) {
                fileFound = true;
                break;
            }
        }
        if (!fileFound) {
            System.out.println("File not found. Please try again");
        } else {
            System.out.print("Copy to: ");
            String copyTo = kb.nextLine().strip();
            if (fileExistsInSimulation(copyTo)) {
                System.out.println("Error: That File Name Already Exits In Simulation, please try again.");
            } else if (copyTo.isBlank() || copyTo.isEmpty() || copyTo.chars().filter(ch -> ch == ' ').count() > 1 || copyTo.chars().filter(ch -> ch == '\n').count() > 0 || copyTo.chars().filter(ch -> ch == ',').count() > 1) {
                System.out.println("Invalid file name, please try again.");
            } else if (copyTo.length() > 8) {
                System.out.println("Filename length can be 8 characters at most, please try again.");
            } else {
                File file = new File("Samples/" + fileInput);
                moveFiletoDisk(file, copyTo);
                System.out.println("Copied from " + fileInput + " to " + copyTo);
            }
        }
    }

    public static void getSimulationFile(boolean display) {
        System.out.println("------------- FILES IN SYSTEM ------------");
        String[] filesInSystem = readFileAllocationTable();
        int length = 0;
        for (String s : filesInSystem) {
            if (s != null) {
                if (s.contains("File")) {
                    String[] e = s.split(" ");
                    System.out.println(e[1]);
                    length++;
                }
            }
        }
        if (length <= 0) {
            System.out.println("No Files stored in System. Please try again later");
            return;
        }
        System.out.print("Copy from: ");
        String fileInput = kb.nextLine().strip();
        boolean fileFound = false;
        for (String f : filesInSystem) {
            if (f != null && f.contains(fileInput)) {
                fileFound = true;
                break;
            }
        }
        if (!fileFound) {
            System.out.println("File not found. Please try again");
        } else {
            System.out.print("Copy to: ");
            String copyTo = kb.nextLine().strip();
            if (fileExistsInSystem(copyTo)) {
                System.out.println("Error: That File Name Already Exits In System, please try again.");
            } else if (copyTo.isBlank() || copyTo.isEmpty() || copyTo.chars().filter(ch -> ch == ' ').count() > 1 || copyTo.chars().filter(ch -> ch == '\n').count() > 0 || copyTo.chars().filter(ch -> ch == ',').count() > 1) {
                System.out.println("Invalid file name, please try again.");
            } else if (copyTo.length() > 8) {
                System.out.println("Filename length can be 8 characters at most, please try again.");
            } else {
                try {
                    byte[] buffer = null;
                    if (allocationType == 1) {
                        buffer = readContiguousFile(fileInput);
                    } else if (allocationType == 2) {
                        buffer = readChainedFile(fileInput);
                    } else if (allocationType == 3) {
                        buffer = readIndex(fileInput);

                    }
                    if (!display) {
                        File targetFile = new File("Samples/" + copyTo);
                        OutputStream outStream;
                        outStream = new FileOutputStream(targetFile);
                        outStream.write(buffer != null ? buffer : new byte[0]);
                        System.out.println("Copied from " + fileInput + " to " + copyTo);
                        outStream.close();
                    } else {
                        String ans = new String(buffer != null ? buffer : new byte[0]);
                        System.out.println("---------- File: " + fileInput + " -----------");
                        System.out.println(ans);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String[] readFileAllocationTable() {
        String fileAllocationTable = new String(diskObject.read(0));
        String[] files = fileAllocationTable.split(",");
        for (int i = 0; i < files.length; i++) {
            String s = files[i];
            if (!s.contains("File")) {
                files[i] = null;
            }
        }
        return files;
    }

    public static boolean fileExistsInSystem(String fileName) {
        File folder = new File("Samples");
        String[] files = folder.list();
        if (files != null) {
            for (String s : files) {
                if (s.toLowerCase().equals(fileName.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean fileExistsInSimulation(String fileName) {
        String[] files = readFileAllocationTable();
        for (String s : files) {
            if (s != null) {
                if (s.toLowerCase().equals(fileName.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void moveFiletoDisk(File file, String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileContents = new byte[(int) file.length()];
            fileInputStream.read(fileContents);
            String s = new String(fileContents);
            //System.out.println("File contents: " + s);
            if (allocationType == 1) {
                contiguousAllocation(fileContents, fileName);
            } else if (allocationType == 2) {
                chainedAllocation(fileContents, fileName);
            } else if (allocationType == 3) {
                indexedAllocation(fileContents, fileName);
            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] readIndex(String fileName) {
        String[] result = findFileInTable(fileName);
        int startBlock = Integer.parseInt(result[2]);
        System.out.println("Start block is " + startBlock);
        int index = 0;
        byte[] indexBlock = diskObject.read(startBlock);
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < indexBlock.length; i++) {
            int pointer = indexBlock[i] & 255;
            if (pointer != 0) {
                indexes.add(pointer);
                System.out.println("Index is " + pointer);
            } else {
                break;
            }
        }
        byte[] data = new byte[blockSize * indexes.size()];
        for (int i : indexes) {
            byte[] block = diskObject.read(i);
            for (int e = 0; e < block.length; e++, index++) {
                data[index] = block[e];
            }
        }
        return data;
    }

    public static void indexedAllocation(byte[] file, String fileName) {
        int blocks = file.length / (blockSize);
        if (file.length % (blockSize) != 0) {
            blocks++;
        }
        byte[] bitmap = diskObject.read(1);
        ArrayList<Integer> openPositions = new ArrayList<>();
        for (int i = 2; i < 256; i++) {
            if (bitmap[i] == 0) {
                openPositions.add(i);
            }
        }
        if (openPositions.size() < blocks) {
            System.out.println("Error: No room to store file - Allocation Type: Chained");
        } else {
            Collections.shuffle(openPositions);
            Collections.shuffle(openPositions);
            Collections.shuffle(openPositions);
            // First 8 blocks or whatever number from list is the random chosen ones
            if (addToFileAllocationTable(fileName, openPositions.get(0), "")) {
                int startIndex = 0;
                byte[] data = new byte[blockSize];
                int index = 0;
                for (int i = 1; i < blocks + 1; i++) {
                    data[i - 1] = (byte) ((int) openPositions.get(i));
                }
                for (int i = blocks; i < blockSize; i++) {
                    data[i] = (byte) ((int) 0);
                }
                diskObject.write(data, openPositions.get(startIndex));
                for (int i = 1; i < blocks + 1; i++) {
                    startIndex++;
                    for (int e = 0; e < blockSize; e++) {
                        if (index < file.length) {
                            data[e] = file[index];
                            index++;
                        } else {
                            data[e] = 0;
                        }
                    }
                    diskObject.write(data, openPositions.get(startIndex));
                    System.out.println("Index: " + openPositions.get(startIndex) + " Block is : " + new String(diskObject.read(openPositions.get(startIndex))));
                }

            }
        }

    }


    public static void chainedAllocation(byte[] file, String fileName) {
        int blocks = file.length / (blockSize - 1);
        if (file.length % (blockSize - 1) != 0) {
            blocks++;
        }
        byte[] bitmap = diskObject.read(1);
        ArrayList<Integer> openPositions = new ArrayList<>();
        for (int i = 2; i < 256; i++) {
            if (bitmap[i] == 0) {
                openPositions.add(i);
            }
        }
        if (openPositions.size() < blocks) {
            System.out.println("Error: No room to store file - Allocation Type: Chained");
        } else {
            Collections.shuffle(openPositions);
            Collections.shuffle(openPositions);
            Collections.shuffle(openPositions);
            // First 8 blocks or whatever number from list is the random chosen ones
            if (addToFileAllocationTable(fileName, openPositions.get(0), "" + blocks)) {
                int openIndex = -1;
                int startIndex;
                byte[] data = new byte[blockSize];
                int index = 0;
                int i = 0;
                while (index < file.length) {
                    if (i == 512 - 1) {
                        openIndex++;
                        startIndex = openPositions.get(openIndex);
                        if (openIndex == blocks - 1) {
                            for (int s = data.length - 1; s < data.length; s++) {
                                if (index < file.length) {
                                    data[s] = file[index];
                                    index++;
                                } else {
                                    data[s] = 0;
                                }
                            }
                        } else if (openIndex < blocks) {
                            byte[] intPointer = new byte[1];
                            intPointer[0] = (byte) ((int) openPositions.get(openIndex + 1));
                            for (int s = data.length - 1, e = 0; s < data.length; s++, e++) {
                                data[s] = intPointer[e];
                            }
                            //System.out.println("Start index: " + startIndex + " Byte to Int " + toInt(intPointer) + " Byte array " + Arrays.toString(intPointer));
                            //System.out.println("Start index " + startIndex + " byte " + intPointer[0] + " int " + (intPointer[0] & 255));
                        }
                        writeToDisk(data, startIndex);
                        System.out.println("last line inside : " + new String(diskObject.read(startIndex)));
                        i = 0;
                    }
                    data[i] = file[index];
                    index++;
                    i++;
                }
                if (index <= file.length) {
                    openIndex++;
                    startIndex = openPositions.get(openIndex);
                    while (i < data.length) {
                        data[i] = 0;
                        i++;
                    }
                    writeToDisk(data, startIndex);
                    System.out.println("last line outside: " + new String(diskObject.read(startIndex)));
                }
            }
        }

    }

    public static byte[] toByteArray(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }

    public static int toInt(byte[] value) {
        return
                ((int) value[0] << 24) +
                        ((int) value[1] << 16) +
                        ((int) value[2] << 8) +
                        (int) value[3];
    }

    public static byte[] readContiguousFile(String fileName) {
        String[] result = findFileInTable(fileName);
        int startBlock = Integer.parseInt(result[2]);
        int blockLength = Integer.parseInt(result[3]);
        byte[] data = new byte[blockSize * blockLength];
        int index = 0;
        for (int i = 0; i < blockLength; i++) {
            byte[] block = diskObject.read(startBlock);
            for (int e = 0; e < block.length; e++) {
                data[index] = block[e];
                index++;
            }
            startBlock++;
        }
        return data;
    }

    public static String[] findFileInTable(String fileName) {
        String[] fAT = readFileAllocationTable();
        String ans = "";
        for (String s : fAT) {
            if (s != null) {
                if (s.contains(fileName)) {
                    ans = s;
                    break;
                }
            }
        }
        String[] result = ans.split(" ");
        return result;
    }

    public static byte[] readChainedFile(String fileName) {
        String[] result = findFileInTable(fileName);
        int startBlock = Integer.parseInt(result[2]);
        int blockLength = Integer.parseInt(result[3]);
        System.out.println("BLock Length is " + blockLength);
        byte[] data = new byte[blockSize * blockLength];
        int index = 0;
        for (int i = 0; i < blockLength; i++) {
            byte[] block = diskObject.read(startBlock);
            if (i == blockLength - 1) {
                for (int e = 0; e < block.length; e++) {
                    data[index] = block[e];
                    index++;
                }
            } else {
                for (int e = 0; e < block.length - 1; e++) {
                    data[index] = block[e];
                    index++;
                }
                byte[] intPointer = new byte[1];
                intPointer[0] = block[block.length - 1];
                int pointer = intPointer[0] & 255;
                startBlock = pointer;
                System.out.println("Mpovin to " + startBlock);
            }
        }
        return data;

    }

    public static void contiguousAllocation(byte[] file, String fileName) {
        int blocks = file.length / blockSize;
        if (file.length % blockSize != 0) {
            blocks++;
        }
        byte[] bitmap = diskObject.read(1);
        int emptyCount = 0;
        int startIndex = -1;
        for (int i = 2; i < bitmap.length; i++) {
            if (bitmap[i] == 0) {
                emptyCount++;
            } else {
                emptyCount = 0;
            }
            if (emptyCount == blocks) {
                startIndex = i - blocks + 1;
                break;
            }
        }
        if (startIndex == -1) {
            System.out.println("Error: No room to store file - Allocation Type: Contiguous");
        } else {
            if (startIndex < 2) {
                System.out.println("Error: Start Index is " + startIndex);
                return;
            }
            if (addToFileAllocationTable(fileName, startIndex, "" + blocks)) {
                byte[] data = new byte[blockSize];
                int index = 0;
                int i = 0;
                while (index < file.length) {
                    if (i == 512) {
                        writeToDisk(data, startIndex);
                        startIndex++;
                        i = 0;
                    }
                    data[i] = file[index];
                    index++;
                    i++;
                }
                if (index <= file.length) {
                    while (i < data.length) {
                        data[i] = 0;
                        i++;
                    }
                    writeToDisk(data, startIndex);
                }
            }
        }

    }

    public static boolean addToFileAllocationTable(String fileName, int startBlock, String blockLength) {
        String fileAllocationTable = new String(diskObject.read(0));
        String tableEntry = "File " + fileName + " " + startBlock + " " + blockLength;
        tableEntry = tableEntry.trim();
        if (fileAllocationTable.charAt(0) == '0') {
            //Hasn't been filled yet
            fileAllocationTable = tableEntry + "," + fileAllocationTable;
            diskObject.write(fileAllocationTable.getBytes(), 0);
        } else {
            String[] files = fileAllocationTable.split(",");
            if (files[files.length - 1].length() >= 1 && files[files.length - 1].charAt(0) == '0' || files[files.length - 1].charAt(0) == ',') {
                files[files.length - 1] = tableEntry;
            }
            StringBuilder fileAllocationTableBuilder = new StringBuilder();
            for (String s : files) {
                fileAllocationTableBuilder.append(s).append(",");
            }
            if (files[files.length - 1].length() >= 1 && files[files.length - 1].charAt(0) == '0') {
                fileAllocationTableBuilder.deleteCharAt(fileAllocationTableBuilder.length() - 1);
            } else {
                fileAllocationTableBuilder.append(tableEntry);
            }
            if (fileAllocationTableBuilder.length() < blockSize) {
                fileAllocationTableBuilder.append(',');
                while (fileAllocationTableBuilder.length() < blockSize) {
                    fileAllocationTableBuilder.append('0');
                }
            }
            byte[] data = fileAllocationTableBuilder.toString().getBytes();
            if (data.length > blockSize) {
                System.out.println("Error: Not enough room in File Allocation Table, cannot write file to disk");
                return false;
            } else {
                writeToDisk(data, 0);
            }
            System.out.println(Arrays.toString(files));
        }
        System.out.println();
        return true;
    }

    public static void writeToDisk(byte[] data, int blockNum) {
        diskObject.write(data, blockNum);
        byte[] bitmap = diskObject.read(1);
        bitmap[blockNum] = 1;
        diskObject.write(bitmap, 1);
    }

    public static String[] displayFilesInFolder() {
        File folder = new File("Samples");

        String[] files = folder.list();

        if (files != null) {
            System.out.println("----------- FILES ----------");
            for (String file : files) {
                System.out.println(file);
            }
        } else {
            System.out.println("There are no files in Samples Folder");
        }
        return files;
    }

    public static int printAllocationMenu() {
        System.out.println("Allocation Types");
        System.out.println("1 - Continuous Allocation");
        System.out.println("2 - Chain Allocation");
        System.out.println("3 - Index Allocation");
        String line;
        while (true) {
            System.out.print("Choose Type 1-3: ");
            line = kb.nextLine();
            try {
                int ans = Integer.parseInt(line);
                if (ans >= 1 && ans <= 3) {
                    return ans;
                } else {
                    System.out.println("Please enter number 1-3");
                }
            } catch (Exception ex) {
                System.out.println("Please enter number 1-3");
            }
        }
    }

    static class Disk {
        private byte[] disk;
        private int blockCount = 256;
        private int blockSize = 512;

        Disk() {
            disk = new byte[blockCount * blockSize];
        }

        void write(byte[] data, int block) {
            int index = block * blockSize;
            for (int i = 0; i < blockSize; i++) {
                disk[index + i] = data[i];
            }
        }

        byte[] read(int block) {
            byte[] data = new byte[blockSize];
            int index = block * blockSize;
            for (int i = 0; i < blockSize; i++) {
                data[i] = disk[index + i];
            }
            return data;
        }

    }
}
