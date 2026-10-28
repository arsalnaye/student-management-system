package com.sms.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * An immutable student record.
 *
 * <p>Instances are created through {@link #builder()} and modified by deriving a
 * new instance from {@link #toBuilder()}. Immutability means a {@code Student}
 * handed to the table model can never be mutated behind the UI's back, which
 * removes a whole class of stale-render bugs.</p>
 *
 * <p>Identity is the database-style surrogate {@link #getId() id}; the
 * {@link #getRollNo() roll number} is the natural business key and is kept
 * unique by the service layer.</p>
 */
public final class Student {

    /** Id used for a record that has not been persisted yet. */
    public static final int UNSAVED_ID = 0;

    private final int id;
    private final String rollNo;
    private final String name;
    private final String email;
    private final String phone;
    private final Department department;
    private final int semester;
    private final double cgpa;
    private final Gender gender;
    private final LocalDate dateOfBirth;
    private final String address;

    private Student(Builder b) {
        this.id = b.id;
        this.rollNo = b.rollNo;
        this.name = b.name;
        this.email = b.email;
        this.phone = b.phone;
        this.department = b.department;
        this.semester = b.semester;
        this.cgpa = b.cgpa;
        this.gender = b.gender;
        this.dateOfBirth = b.dateOfBirth;
        this.address = b.address;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return a builder pre-loaded with this student's values. */
    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .rollNo(rollNo)
                .name(name)
                .email(email)
                .phone(phone)
                .department(department)
                .semester(semester)
                .cgpa(cgpa)
                .gender(gender)
                .dateOfBirth(dateOfBirth)
                .address(address);
    }

    public int getId() {
        return id;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Department getDepartment() {
        return department;
    }

    public int getSemester() {
        return semester;
    }

    public double getCgpa() {
        return cgpa;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    /** @return {@code true} when this record has never been stored. */
    public boolean isNew() {
        return id == UNSAVED_ID;
    }

    /** @return age in whole years on {@code today}, or {@code -1} if no date of birth is set. */
    public int ageOn(LocalDate today) {
        if (dateOfBirth == null || today == null) {
            return -1;
        }
        return Period.between(dateOfBirth, today).getYears();
    }

    /** @return a coarse academic standing derived from the CGPA. */
    public String grade() {
        if (cgpa >= 9.0) {
            return "Outstanding";
        }
        if (cgpa >= 7.5) {
            return "Excellent";
        }
        if (cgpa >= 6.0) {
            return "Good";
        }
        if (cgpa >= 5.0) {
            return "Average";
        }
        return "At risk";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Student)) {
            return false;
        }
        Student other = (Student) o;
        if (id != UNSAVED_ID || other.id != UNSAVED_ID) {
            return id == other.id;
        }
        return Objects.equals(rollNo, other.rollNo);
    }

    @Override
    public int hashCode() {
        return id != UNSAVED_ID ? Integer.hashCode(id) : Objects.hashCode(rollNo);
    }

    @Override
    public String toString() {
        return "Student{" + id + ", " + rollNo + ", " + name + ", " + department + ", sem " + semester + '}';
    }

    /** Fluent builder; the only way to create a {@link Student}. */
    public static final class Builder {

        private int id = UNSAVED_ID;
        private String rollNo = "";
        private String name = "";
        private String email = "";
        private String phone = "";
        private Department department = Department.CSE;
        private int semester = 1;
        private double cgpa;
        private Gender gender = Gender.OTHER;
        private LocalDate dateOfBirth;
        private String address = "";

        private Builder() {
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder rollNo(String rollNo) {
            this.rollNo = rollNo;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder department(Department department) {
            this.department = department;
            return this;
        }

        public Builder semester(int semester) {
            this.semester = semester;
            return this;
        }

        public Builder cgpa(double cgpa) {
            this.cgpa = cgpa;
            return this;
        }

        public Builder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Student build() {
            return new Student(this);
        }
    }
}
