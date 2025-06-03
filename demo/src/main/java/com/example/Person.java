package com.example;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Person class for RoadRegistry platform.
 * Stores person data and manages demerit points.
 *
 * Note: since the "address" itself contains pipe characters (e.g. "20|King
 * St|Melbourne|Victoria|Australia"),
 * we cannot simply do line.split("\\|") and assume fixed indexes. Instead, we
 * treat
 * the very last two '|'‐delimited tokens as birthDate and suspended, and
 * everything before
 * that (joined back together) is the address.
 */
public class Person {
    private String id;
    private String firstName;
    private String lastName;
    private String address; // e.g. "20|King St|Melbourne|Victoria|Australia"
    private String birthDate; // "DD-MM-YYYY"
    private boolean isSuspended;

    private static final String PERSONS_FILE = "persons.txt";
    private static final String DEMERITS_FILE = "demerits.txt";

    private static final DateTimeFormatter DTF = DateTimeFormatter
            .ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);.withResolverStyle(ResolverStyle.STRICT);

    public Person(String id, String firstName, String lastName, String address, String birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.birthDate = birthDate;

    }

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
     * Adds a person to persons.txt if all validations pass.
     * Returns true if added successfully, false otherwise.
     */
    public static boolean addPerson(Person person) {
        // 1) Validate ID pattern
        String id = person.id;
        Pattern idPattern = Pattern.compile(
                "^(?=.{10}$)(?=.*[^A-Za-z0-9].*[^A-Za-z0-9])[2-9]{2}.{6}[A-Z]{2}$");
        Matcher idMatcher = idPattern.matcher(id);
        if (!idMatcher.matches()) {
            return false;
        }

        // 2) Validate address: must have exactly 5 subfields when split on '|',
        // and the 4th subfield (index 3) must be "Victoria".
        String[] addrParts = person.address.split("\\|");
        if (addrParts.length != 5 || !addrParts[3].equals("Victoria")) {
            return false;
        }

        // 3) Validate birthDate format
        try {
            LocalDate.parse(person.birthDate, DTF);
        } catch (DateTimeParseException ex) {
            return false;
        }

        // 4) Append to persons.txt in the form:
        // id|firstName|lastName|<address-string>|birthDate|false
        // (note: address-string already contains internal '|' chars)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PERSONS_FILE, true))) {
            // We do NOT re-split address here; we store it verbatim.
            String line = String.join("|",
                    person.id,
                    person.firstName,
                    person.lastName,
                    person.address,
                    person.birthDate,
                    "false");
            writer.write(line);
            writer.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds demerit points for a person. Validates date and point range.
     * Updates suspension status if threshold exceeded.
     *
     * Returns "Success" if added, "Failed" otherwise.
     */
    public static String addDemeritPoints(String id, String offenseDate, int points) {
        // 1) Parse and validate offenseDate
        LocalDate offense;
        try {
            offense = LocalDate.parse(offenseDate, DTF);
        } catch (DateTimeParseException ex) {
            return "Failed";
        }

        // 2) Validate points range 1..6
        if (points < 1 || points > 6) {
            return "Failed";
        }

        // 3) Look up the person in persons.txt to fetch birthDate and current
        // suspension state
        LocalDate birth = null;
        boolean currentlySuspended = false;
        boolean found = false;

        try (BufferedReader personReader = new BufferedReader(new FileReader(PERSONS_FILE))) {
            String line;
            while ((line = personReader.readLine()) != null) {
                // Split into tokens. Because address has internal '|', we do a full split, then
                // know the last two tokens are birthDate and suspended.
                String[] fullParts = line.split("\\|");
                int n = fullParts.length;
                if (fullParts[0].equals(id)) {
                    // birthDate is the second‐to‐last token:
                    birth = LocalDate.parse(fullParts[n - 2], DTF);
                    currentlySuspended = Boolean.parseBoolean(fullParts[n - 1]);
                    found = true;
                    break;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "Failed";
        }

        if (!found) {
            return "Failed"; // no person with that ID
        }

        // 4) Compute age at offense date
        int age = offense.getYear() - birth.getYear();

        // 5) Sum all points in the last 2 years (inclusive of this offense)
        LocalDate cutoff = offense.minusYears(2);
        int totalPoints = points; // start with the new offense's points
        try (BufferedReader demReader = new BufferedReader(new FileReader(DEMERITS_FILE))) {
            String dline;
            while ((dline = demReader.readLine()) != null) {
                String[] dparts = dline.split("\\|");
                // Format in demerits.txt is: id|offenseDate|points
                if (!dparts[0].equals(id)) {
                    continue;
                }
                LocalDate dDate = LocalDate.parse(dparts[1], DTF);
                int pts = Integer.parseInt(dparts[2]);
                // If dDate is in [cutoff .. offense], include it
                if ((!dDate.isBefore(cutoff)) && (!dDate.isAfter(offense))) {
                    totalPoints += pts;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "Failed";
        }

        // 6) Decide threshold
        int threshold = (age < 21) ? 6 : 12;
        boolean willSuspend = totalPoints > threshold;

        // 7) Append this offense record to demerits.txt
        try (BufferedWriter demWriter = new BufferedWriter(new FileWriter(DEMERITS_FILE, true))) {
            String demLine = String.join("|", id, offenseDate, Integer.toString(points));
            demWriter.write(demLine);
            demWriter.newLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "Failed";
        }

        // 8) If suspension status flips from false to true, update persons.txt
        if (willSuspend && !currentlySuspended) {
            try {
                updateSuspensionStatus(id, true);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return "Failed";
            }
        }

        return "Success";
    }

    /**
     * Helper to flip the suspended flag in persons.txt for a given ID.
     */
    private static void updateSuspensionStatus(String id, boolean suspend) throws IOException {
        File file = new File(PERSONS_FILE);
        if (!file.exists()) {
            return;
        }

        List<String> allLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fullParts = line.split("\\|");
                int n = fullParts.length;
                if (fullParts[0].equals(id)) {
                    // Re‐construct the last token (suspended) to "true"/"false"
                    fullParts[n - 1] = Boolean.toString(suspend);
                    // Re‐join everything with '|'
                    StringBuilder sb = new StringBuilder(fullParts[0]);
                    for (int i = 1; i < n; i++) {
                        sb.append("|").append(fullParts[i]);
                    }
                    allLines.add(sb.toString());
                } else {
                    allLines.add(line);
                }
            }
        }

        // Overwrite the file with updated lines
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (String out : allLines) {
                writer.write(out);
                writer.newLine();
            }
        }
    }

    // ------------------------------------------------------------------------
    // Note: The updatePersonalDetails(...) method below still assumes you want to
    // do
    // things like “if under 18, address can’t change,” etc. If you plan to use
    // it, you will also need to adjust its splitting logic in the same way—
    // always treating the last two tokens as birthDate+suspended, and rebuilding
    // address from everything in between. The version below is simply a direct
    // fix of your original, applying the same “last-two tokens are date+suspend”
    // approach. If you do not call updatePersonalDetails in your tests, you can
    // leave it as-is or remove it.
    // ------------------------------------------------------------------------

    /**
     * Updates personal details of an existing person in persons.txt.
     * Enforces rules:
     * - If person is under 18, address cannot be changed.
     * - If birthdate changes, no other detail can change.
     * - If original ID’s first digit is even, ID cannot change.
     * Returns true if update succeeds, false if any rule is violated or I/O fails.
     */
    public static boolean updatePersonalDetails(Person updatedPerson) {
        String targetId = updatedPerson.id;
        String targetBirth = updatedPerson.birthDate;
        boolean birthdayChanged = false;

        try {
            File file = new File(PERSONS_FILE);
            if (!file.exists()) {
                return false;
            }

            List<String> allLines = new ArrayList<>();
            boolean found = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fullParts = line.split("\\|");
                    int n = fullParts.length;

                    if (fullParts[0].equals(targetId)) {
                        found = true;

                        // Reconstruct original birthDate and original address
                        String origBirth = fullParts[n - 2];
                        // address is everything between index 3 .. n-3, joined with '|'
                        StringBuilder origAddrSb = new StringBuilder(fullParts[3]);
                        for (int i = 4; i <= n - 3; i++) {
                            origAddrSb.append("|").append(fullParts[i]);
                        }
                        String origAddress = origAddrSb.toString();

                        String origFirst = fullParts[1];
                        String origLast = fullParts[2];
                        int origFirstDigit = Character.getNumericValue(origIdFirstDigit(fullParts[0]));
                        int yearNow = LocalDate.now().getYear();
                        int yearBirth = LocalDate.parse(origBirth, DTF).getYear();
                        int age = yearNow - yearBirth;

                        // RULE A: if under 18, address cannot change
                        if (age < 18 && !origAddress.equals(updatedPerson.address)) {
                            return false;
                        }

                        // RULE B: if birthdate changed, NO other field can change
                        if (!origBirth.equals(updatedPerson.birthDate)) {
                            birthdayChanged = true;
                        }
                        if (birthdayChanged) {
                            if (!fullParts[0].equals(updatedPerson.id) ||
                                    !origFirst.equals(updatedPerson.firstName) ||
                                    !origLast.equals(updatedPerson.lastName) ||
                                    !origAddress.equals(updatedPerson.address)) {
                                return false;
                            }
                        }

                        // RULE C: if original first digit of ID is even, cannot change ID
                        if ((origFirstDigit % 2 == 0) && !fullParts[0].equals(updatedPerson.id)) {
                            return false;
                        }

                        // Validate updated fields exactly as in addPerson (except suspended flag)
                        // (We only want to validate id/firstname/lastname/address/birthdate format,
                        // not actually append a new line—so do a “fake” Person and run the same
                        // validation.)
                        Person validator = new Person(
                                updatedPerson.id,
                                updatedPerson.firstName,
                                updatedPerson.lastName,
                                updatedPerson.address,
                                updatedPerson.birthDate);
                        // If validation fails, do NOT proceed.
                        if (!validatePersonFormat(validator)) {
                            return false;
                        }

                        // Rebuild this line with updated fields, preserving the original suspended
                        // flag:
                        String suspendedState = fullParts[n - 1];
                        StringBuilder newLineSb = new StringBuilder(updatedPerson.id);
                        newLineSb.append("|").append(updatedPerson.firstName)
                                .append("|").append(updatedPerson.lastName)
                                .append("|").append(updatedPerson.address)
                                .append("|").append(updatedPerson.birthDate)
                                .append("|").append(suspendedState);
                        allLines.add(newLineSb.toString());
                    } else {
                        // Keep everybody else unchanged
                        allLines.add(line);
                    }
                }
            }

            if (!found) {
                return false;
            }

            // Overwrite the file with updated lines
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                for (String outLine : allLines) {
                    writer.write(outLine);
                    writer.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper to validate a Person’s fields without writing to file.
     * Returns true if id/address/birthDate all pass the same checks as addPerson.
     */
    private static boolean validatePersonFormat(Person person) {
        // 1) ID pattern
        Pattern idPattern = Pattern.compile(
                "^(?=.{10}$)(?=.*[^A-Za-z0-9].*[^A-Za-z0-9])[2-9]{2}.{6}[A-Z]{2}$");
        Matcher idMatcher = idPattern.matcher(person.id);
        if (!idMatcher.matches()) {
            return false;
        }

        // 2) Address must split into 5 parts and part[3] = "Victoria"
        String[] addrParts = person.address.split("\\|");
        if (addrParts.length != 5 || !addrParts[3].equals("Victoria")) {
            return false;
        }

        // 3) birthDate parse
        try {
            LocalDate.parse(person.birthDate, DTF);
        } catch (DateTimeParseException ex) {
            return false;
        }

        return true;
    }

    /**
     * Returns the first character of an ID as a digit (e.g. '2' → 2).
     * We assume ID always begins with a digit here (per your regex).
     */
    private static char origIdFirstDigit(String id) {
        return id.charAt(0);
    }
}
// ...existing code...