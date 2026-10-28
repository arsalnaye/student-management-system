package com.sms.util;

import com.sms.model.Department;
import com.sms.model.Gender;
import com.sms.model.Student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo records used the first time the app runs against an empty data file, so
 * the grid, the filters and the dashboard have something to show.
 */
public final class SampleData {

    private SampleData() {
    }

    /** @return eight fictional students spread across departments and semesters. */
    public static List<Student> students() {
        List<Student> list = new ArrayList<Student>();
        list.add(build("23CSE101", "Aarav Sharma", "aarav.sharma@college.edu", "9876543210",
                Department.CSE, 5, 8.92, Gender.MALE, LocalDate.of(2004, 3, 5),
                "12 Nehru Road, Pune, Maharashtra"));
        list.add(build("23CSE118", "Diya Nair", "diya.nair@college.edu", "9812345670",
                Department.CSE, 5, 9.41, Gender.FEMALE, LocalDate.of(2004, 7, 19),
                "88 MG Road, Kochi, Kerala"));
        list.add(build("23IT204", "Rohan Verma", "rohan.verma@college.edu", "9900112233",
                Department.IT, 3, 7.64, Gender.MALE, LocalDate.of(2005, 1, 22),
                "5 Park Street, Kolkata, West Bengal"));
        list.add(build("22ECE310", "Ishita Rao", "ishita.rao@college.edu", "9765432180",
                Department.ECE, 7, 8.15, Gender.FEMALE, LocalDate.of(2003, 11, 2),
                "27 Jubilee Hills, Hyderabad, Telangana"));
        list.add(build("22MECH415", "Karan Singh", "karan.singh@college.edu", "9123456780",
                Department.MECH, 7, 6.48, Gender.MALE, LocalDate.of(2003, 6, 14),
                "104 Model Town, Ludhiana, Punjab"));
        list.add(build("24CIVIL022", "Meera Joshi", "meera.joshi@college.edu", "9556677889",
                Department.CIVIL, 1, 7.05, Gender.FEMALE, LocalDate.of(2006, 2, 27),
                "60 Station Road, Nashik, Maharashtra"));
        list.add(build("23EEE207", "Sahil Khan", "sahil.khan@college.edu", "9334455667",
                Department.EEE, 3, 5.72, Gender.MALE, LocalDate.of(2004, 9, 9),
                "9 Civil Lines, Lucknow, Uttar Pradesh"));
        list.add(build("22MBA509", "Ananya Iyer", "ananya.iyer@college.edu", "9440011223",
                Department.MBA, 3, 8.77, Gender.FEMALE, LocalDate.of(2002, 12, 30),
                "45 Anna Salai, Chennai, Tamil Nadu"));
        return list;
    }

    private static Student build(String rollNo, String name, String email, String phone,
                                 Department department, int semester, double cgpa,
                                 Gender gender, LocalDate dob, String address) {
        return Student.builder()
                .rollNo(rollNo)
                .name(name)
                .email(email)
                .phone(phone)
                .department(department)
                .semester(semester)
                .cgpa(cgpa)
                .gender(gender)
                .dateOfBirth(dob)
                .address(address)
                .build();
    }
}
