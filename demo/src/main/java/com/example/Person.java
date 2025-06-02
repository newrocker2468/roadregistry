package com.example;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// ...existing code...
public class Person {
    private String id;
    private String firstName;
    private String lastName;
    private String address; // Format: StreetNumber|Street|City|Victoria|Country
private String birthDate; // Format: DD-MM-YYYY

private static final String PERSONS_FILE = "persons.txt";
    private static final String DEMERITS_FILE = "demerits.txt";

    private static final DateTimeFormatter DTF = DateTimeFormatter
            .ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public Person(String id, String firstName, String lastName, String address, String birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.birthDate = birthDate;
        
    }

    // ...existing code...

public static boolean addPerson(Person person) {
    // Validate ID: must be exactly 10 characters and contain only allowed characters
    if (person.id == null || person.id.length() != 10 || !person.id.matches("[A-Za-z0-9@#$%&]+")) {
        return false;
    }
    // Validate address: must be "StreetNumber|Street|City|Victoria|Country"
    String[] parts = person.address.split("\\|");
    if (parts.length != 5 || !parts[3].equals("Victoria")) {
        return false;
    }
    // Validate birthDate format
    try {
        LocalDate.parse(person.birthDate, DTF);
    } catch (DateTimeParseException e) {
        return false;
    }
    // ...existing code for writing to file...
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(PERSONS_FILE, true))) {
        String[] addrParts = person.address.split("\\|");
        String line = String.join("|",
            person.id,
            person.firstName,
            person.lastName,
            addrParts[0], addrParts[1], addrParts[2], addrParts[3], addrParts[4],
            person.birthDate,
            "false"
        );
        writer.write(line);
        writer.newLine();
        return true;
    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }
}

    public String getId() {
        return this.id;
    }
    public static boolean updatePersonalDetails(Person updatedPerson) {
        String targetId = updatedPerson.id;
        boolean birthdayChanged = false;
        try {
            File file = new File(PERSONS_FILE);
            if (!file.exists())
                return false;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            boolean found = false;
            List<String> allLines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts[0].equals(targetId) || parts[0].equals(updatedPerson.id)) {
                    found = true;
                    String origId = parts[0];
                    String origBirth = parts[8];
                    String origAddress = String.join("|", parts[3], parts[4], parts[5], parts[6], parts[7]);
                    String origFirst = parts[1], origLast = parts[2];
                    int age = LocalDate.now().getYear() - LocalDate.parse(origBirth, DTF).getYear();

                    if (age < 18 && !origAddress.equals(updatedPerson.address)) {
                        reader.close();
                        return false;
                    }
                    if (!origBirth.equals(updatedPerson.birthDate)) {
                        birthdayChanged = true;
                    }
                    if (birthdayChanged) {
                        if (!origId.equals(updatedPerson.id) ||
                                !origFirst.equals(updatedPerson.firstName) ||
                                !origLast.equals(updatedPerson.lastName) ||
                                !origAddress.equals(updatedPerson.address)) {
                            reader.close();
                            return false;
                        }
                    }
                    if (Character.getNumericValue(origId.charAt(0)) % 2 == 0 &&
                            !origId.equals(updatedPerson.id)) {
                        reader.close();
                        return false;
                    }
                    if (!addPerson(new Person(updatedPerson.id, updatedPerson.firstName,
                            updatedPerson.lastName, updatedPerson.address, updatedPerson.birthDate))) {
                        reader.close();
                        return false;
                    }
                    String suspendedState = parts[9];
                    String[] addrParts = updatedPerson.address.split("\\|");
                    String newLine = String.join("|",
                        updatedPerson.id, updatedPerson.firstName, updatedPerson.lastName,
                        addrParts[0], addrParts[1], addrParts[2], addrParts[3], addrParts[4],
                        updatedPerson.birthDate, suspendedState
                    );
                    allLines.add(newLine);
                } else {
                    allLines.add(line);
                }
            }
            reader.close();
            if (!found)
                return false;
            BufferedWriter writer = new BufferedWriter(new FileWriter(PERSONS_FILE));
            for (String outLine : allLines) {
                writer.write(outLine);
                writer.newLine();
            }
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String addDemeritPoints(String id, String offenseDate, int points) {
        LocalDate offense;
        try {
            offense = LocalDate.parse(offenseDate, DTF);
        } catch (DateTimeParseException ex) {
            return "Failed";
        }
        if (points < 1 || points > 6) {
            return "Failed";
        }
        try {
            BufferedReader personReader = new BufferedReader(new FileReader(PERSONS_FILE));
            String line;
            LocalDate birth = null;
            boolean currentlySuspended = false;
            boolean found = false;
            while ((line = personReader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts[0].equals(id)) {
                    birth = LocalDate.parse(parts[8], DTF);
                    currentlySuspended = Boolean.parseBoolean(parts[9]);
                    found = true;
                    break;
                }
            }
            personReader.close();
            if (!found)
                return "Failed";

            int age = offense.getYear() - birth.getYear();
            LocalDate cutoff = offense.minusYears(2);
            BufferedReader demeritReader = new BufferedReader(new FileReader(DEMERITS_FILE));
            String dline;
            int totalPoints = points;
            while ((dline = demeritReader.readLine()) != null) {
                String[] dparts = dline.split("\\|");
                if (!dparts[0].equals(id))
                    continue;
                LocalDate dDate = LocalDate.parse(dparts[1], DTF);
                int pts = Integer.parseInt(dparts[2]);
                if (!dDate.isBefore(cutoff) && !dDate.isAfter(offense)) {
                    totalPoints += pts;
                }
            }
            demeritReader.close();

            int threshold = (age < 21) ? 6 : 12;
            boolean willSuspend = totalPoints > threshold;

            BufferedWriter demWriter = new BufferedWriter(new FileWriter(DEMERITS_FILE, true));
            String demLine = String.join("|", id, offenseDate, Integer.toString(points));
            demWriter.write(demLine);
            demWriter.newLine();
            demWriter.close();

            if (willSuspend && !currentlySuspended) {
                updateSuspensionStatus(id, true);
            }
            return "Success";
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed";
        }
    }

    private static void updateSuspensionStatus(String id, boolean suspend) throws IOException {
        File file = new File(PERSONS_FILE);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        List<String> lines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\|");
            if (parts[0].equals(id)) {
                parts[9] = Boolean.toString(suspend);
                line = String.join("|", parts);
            }
            lines.add(line);
        }
        reader.close();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String out : lines) {
            writer.write(out);
            writer.newLine();
        }
        writer.close();
    }
}
// ...existing code...