package com.example;

import java.time.format.ResolverStyle;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Person class for RoadRegistry platform.
 * Stores person data and manages demerit points.
 */
public class Person {
    private String id;
    private String firstName;
    private String lastName;
    private String address; // Format: StreetNumber|Street|City|Victoria|Country
    private String birthDate; // Format: DD-MM-YYYY
    private boolean isSuspended;
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
        this.isSuspended = false;
    }

    // Getter for ID (for comparison)
    public String getId() {
        return id;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    /**
     * Adds a person to the registry file if all validations pass.
     * Checks ID format, address format, and birthdate format.
     * Returns true if added successfully, false otherwise.
     */
    public static boolean addPerson(Person person) {
        // Validate ID: exactly 10 chars, first two [2-9], at least 2 special chars in
        // 3-8, last two uppercase
        String id = person.id;
        // Regex breakdown:
        // ^ : start
        // (?=.{10}$) : ensure length is 10
        // (?=.*[^A-Za-z0-9].*[^A-Za-z0-9]) : at least 2 non-alphanumeric anywhere
        // [2-9]{2} : first two chars between 2 and 9
        // .{6} : next 6 any chars (we already ensure specials via lookahead)
        // [A-Z]{2} : last two uppercase letters
        // $ : end
        Pattern idPattern = Pattern.compile("^(?=.{10}$)(?=.*[^A-Za-z0-9].*[^A-Za-z0-9])[2-9]{2}.{6}[A-Z]{2}$");
        Matcher idMatcher = idPattern.matcher(id);
        if (!idMatcher.matches()) {
            // ID did not match criteria
            return false;
        }

        // Validate address: must be "StreetNumber|Street|City|Victoria|Country"
        String[] parts = person.address.split("\\|");
        if (parts.length != 5 || !parts[3].equals("Victoria")) {
            // Invalid format or state not "Victoria"
            return false;
        }

        // Validate birthdate format: "DD-MM-YYYY"
        try {
            LocalDate.parse(person.birthDate, DTF);
        } catch (DateTimeParseException ex) {
            return false;
        }

        // All validations passed; append to persons file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PERSONS_FILE, true))) {
            // Example file line: id|firstName|lastName|address|birthDate|suspended
            String line = String.join("|",
                    person.id, person.firstName, person.lastName, person.address, person.birthDate, "false");
            writer.write(line);
            writer.newLine();
            return true;
        } catch (IOException e) {
            // I/O error writing file
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates personal details of an existing person in persons.txt.
     * Enforces rules:
     * - If person is under 18, address cannot be changed.
     * - If birthday changes, no other details can change.
     * - If original ID's first char is even, ID cannot change.
     * Returns true if update succeeds, false if any rule is violated or I/O fails.
     */
    public static boolean updatePersonalDetails(Person updatedPerson) {
        String targetId = updatedPerson.id;
        String targetBirth = updatedPerson.birthDate;
        boolean birthdayChanged = false;
        try {
            // Read all lines, modify the matching entry, then rewrite file
            File file = new File(PERSONS_FILE);
            if (!file.exists())
                return false;

            // Temporary storage for updated lines
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            boolean found = false;
            List<String> allLines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts[0].equals(targetId) || parts[0].equals(updatedPerson.id)) {
                    // Found the person entry
                    found = true;
                    String origId = parts[0];
                    String origBirth = parts[4];
                    String origAddress = parts[3];
                    String origFirst = parts[1], origLast = parts[2];
                    int age = LocalDate.now().getYear() - LocalDate.parse(origBirth, DTF).getYear();

                    // Check age-based rule: under 18 cannot change address
                    if (age < 18 && !origAddress.equals(updatedPerson.address)) {
                        reader.close();
                        return false;
                    }
                    // Check birthday-change rule
                    if (!origBirth.equals(updatedPerson.birthDate)) {
                        birthdayChanged = true;
                    }
                    // If birthday changed and any other changed, fail
                    if (birthdayChanged) {
                        if (!origId.equals(updatedPerson.id) ||
                                !origFirst.equals(updatedPerson.firstName) ||
                                !origLast.equals(updatedPerson.lastName) ||
                                !origAddress.equals(updatedPerson.address)) {
                            reader.close();
                            return false;
                        }
                    }
                    // Check ID parity rule: if original first digit even, cannot change ID
                    if (Character.getNumericValue(origId.charAt(0)) % 2 == 0 &&
                            !origId.equals(updatedPerson.id)) {
                        reader.close();
                        return false;
                    }
                    // Validate updated fields same as addPerson (except suspended flag)
                    if (!addPerson(new Person(updatedPerson.id, updatedPerson.firstName,
                            updatedPerson.lastName, updatedPerson.address, updatedPerson.birthDate))) {
                        reader.close();
                        return false;
                    }
                    // Write updated record (keep original suspension state)
                    String suspendedState = parts[5];
                    String newLine = String.join("|", updatedPerson.id, updatedPerson.firstName,
                            updatedPerson.lastName, updatedPerson.address, updatedPerson.birthDate,
                            suspendedState);
                    allLines.add(newLine);
                } else {
                    // Keep other entries unchanged
                    allLines.add(line);
                }
            }
            reader.close();
            if (!found)
                return false;
            // Rewrite entire file with updated lines
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

    /**
     * Adds demerit points for a person. Validates date and point range.
     * Sets isSuspended if thresholds are exceeded (6 if under 21, else 12 in 2
     * years).
     * Returns "Success" if added, "Failed" otherwise.
     */
    public static String addDemeritPoints(String id, String offenseDate, int points) {
        // Validate date format
        LocalDate offense;
        try {
            offense = LocalDate.parse(offenseDate, DTF);
        } catch (DateTimeParseException ex) {
            return "Failed";
        }
        // Validate points 1-6
        if (points < 1 || points > 6) {
            return "Failed";
        }
        try {
            // Read person to get birthdate and current isSuspended
            BufferedReader personReader = new BufferedReader(new FileReader(PERSONS_FILE));
            String line;
            LocalDate birth = null;
            boolean currentlySuspended = false;
            boolean found = false;
            while ((line = personReader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts[0].equals(id)) {
                    birth = LocalDate.parse(parts[4], DTF);
                    currentlySuspended = Boolean.parseBoolean(parts[5]);
                    found = true;
                    break;
                }
            }
            personReader.close();
            if (!found)
                return "Failed";

            // Calculate age at offense date
            int age = offense.getYear() - birth.getYear();
            // Sum points in past 2 years (current offense date back 2 years)
            LocalDate cutoff = offense.minusYears(2);
            BufferedReader demeritReader = new BufferedReader(new FileReader(DEMERITS_FILE));
            String dline;
            int totalPoints = points; // include this offense
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

            // Determine threshold
            int threshold = (age < 21) ? 6 : 12;
            boolean willSuspend = totalPoints > threshold;

            // Append demerit record: id|offenseDate|points
            BufferedWriter demWriter = new BufferedWriter(new FileWriter(DEMERITS_FILE, true));
            String demLine = String.join("|", id, offenseDate, Integer.toString(points));
            demWriter.write(demLine);
            demWriter.newLine();
            demWriter.close();

            // If suspension status changed to true, update person record
            if (willSuspend && !currentlySuspended) {
                // Update suspended status in persons file
                updateSuspensionStatus(id, true);
            }
            return "Success";
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed";
        }
    }

    // Helper to update suspension flag in persons.txt
    private static void updateSuspensionStatus(String id, boolean suspend) throws IOException {
        File file = new File(PERSONS_FILE);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        List<String> lines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\|");
            if (parts[0].equals(id)) {
                parts[5] = Boolean.toString(suspend);
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
