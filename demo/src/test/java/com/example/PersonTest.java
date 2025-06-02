package com.example;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class PersonTest {

    @Test
    void testAddPerson_ValidData_ReturnsTrue() {
        Person p = new Person("23#$abCDEF", "Alice", "Smith",
                              "123|Main St|Melbourne|Victoria|Australia", "01-01-2000");
        assertTrue(Person.addPerson(p));
    }

    @Test
    void testAddPerson_InvalidID_ReturnsFalse() {
        Person p = new Person("12abcdefGH", "Bob", "Jones",
                              "10|Oak Rd|Melbourne|Victoria|Australia", "31-12-1999");
        assertFalse(Person.addPerson(p));
    }

    @Test
    void testAddPerson_InvalidBirthday_ReturnsFalse() {
        Person p = new Person("56##ghIJKL", "Carol", "Lee",
                              "77|Pine Ave|Melbourne|Victoria|Australia", "30-02-2010");
        assertFalse(Person.addPerson(p));
    }
// ... additional tests ...

    @Test
void testUpdateDetails_Under18ChangeAddress_Fails() {
    // Person under 18 (DOB in 2010) tries to change address
    Person original = new Person("33@@xyZZ", "Young", "Driver",
                                 "1|First St|Melbourne|Victoria|Australia", "01-01-2010");
    Person.updatePersonalDetails(original); // ensure record exists
    Person updated = new Person("33@@xyZZ", "Young", "Driver",
                                "2|Second St|Melbourne|Victoria|Australia", "01-01-2010");
    assertFalse(Person.updatePersonalDetails(updated));
}

@Test
void testUpdateDetails_ChangeBirthdayAndName_Fails() {
    Person original = new Person("55##ghAB", "Old", "Name",
                                 "3|Third St|Melbourne|Victoria|Australia", "15-05-1995");
    Person.updatePersonalDetails(original);
    // Change birthdate and name simultaneously (invalid)
    Person updated = new Person("55##ghAB", "New", "Name",
                                "3|Third St|Melbourne|Victoria|Australia", "16-06-1995");
    assertFalse(Person.updatePersonalDetails(updated));
}

@Test
void testUpdateDetails_EvenIdChangeID_Fails() {
    Person original = new Person("44%%ffGG", "Even", "ID",
                                 "4|Fourth St|Melbourne|Victoria|Australia", "10-10-1990");
    Person.updatePersonalDetails(original);
    // Original ID starts with '4' (even), try to change it
    Person updated = new Person("45%%ffGG", "Even", "ID",
                                "4|Fourth St|Melbourne|Victoria|Australia", "10-10-1990");
    assertFalse(Person.updatePersonalDetails(updated));
}

    @BeforeEach
    void setUp() throws IOException {
        // Delete old test files if they exist
        Path personFile = Paths.get("persons.txt");
        Files.deleteIfExists(personFile);               // deletes file if it exists:contentReference[oaicite:1]{index=1}
        Files.createFile(personFile);                   // create a new empty persons.txt

        Path demeritFile = Paths.get("demerits.txt");
        Files.deleteIfExists(demeritFile);              // deletes file if it exists:contentReference[oaicite:2]{index=2}
        Files.createFile(demeritFile);                  // create a new empty demerits.txt
    }

 @Test
void testAddDemerits_Under21BelowThreshold_NoSuspend() {
    Person p = new Person("23@#xyLMNO", "Teen", "Driver",
        "5|Fifth St|Melbourne|Victoria|Australia", "01-01-2007");

    assertTrue(Person.addPerson(p)); // ✅ This MUST succeed

    String result = Person.addDemeritPoints("23@#xyLMNO", "01-01-2024", 4); // ✅ DD-MM-YYYY format
    assertEquals("Success", result);
}

@Test
void testAddDemerits_InvalidPoints_Fails() {
    Person p = new Person("88&&yyBB", "Risky", "Road",
                          "6|Sixth St|Melbourne|Victoria|Australia", "01-01-1990");
    Person.addPerson(p);
    String result = Person.addDemeritPoints(p.getId(), "15-07-2024", 7);
    assertEquals("Failed", result);
}

@Test
void testAddDemerits_ThresholdExceeded_Suspends() {
    Person p = new Person("34$%abXYZA", "Tom", "Jones",
        "20|King St|Melbourne|Victoria|Australia", "01-01-2005");

    assertTrue(Person.addPerson(p)); // Must be added

    // Add 2 past records (optional if using file), then final push
    assertEquals("Success", Person.addDemeritPoints("34$%abXYZA", "01-01-2023", 4));
    assertEquals("Success", Person.addDemeritPoints("34$%abXYZA", "01-06-2024", 3));
    String result = Person.addDemeritPoints("34$%abXYZA", "01-12-2024", 2); // Should push over threshold

    assertEquals("Success", result);
}

}
