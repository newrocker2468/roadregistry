package com.example;
import org.junit.jupiter.api.Test;
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
}
