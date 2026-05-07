package com.ontoevolve.domain.education.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "students")
public class StudentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String school;
    private String sex;
    private int age;
    private String address;
    private String famsize;
    private String pstatus;
    private int medu;
    private int fedu;
    private String mjob;
    private String fjob;
    private String reason;
    private String guardian;
    private int traveltime;
    private int studytime;
    private int failures;
    private String schoolsup;
    private String famsup;
    private String paid;
    private String activities;
    private String nursery;
    private String higher;
    private String internet;
    private String romantic;
    private int famrel;
    private int freetime;
    private int goout;
    private int dalc;
    private int walc;
    private int health;
    private int absences;
    private int g1;
    private int g2;
    private int g3;
    private String studentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getFamsize() { return famsize; }
    public void setFamsize(String famsize) { this.famsize = famsize; }
    public String getPstatus() { return pstatus; }
    public void setPstatus(String pstatus) { this.pstatus = pstatus; }
    public int getMedu() { return medu; }
    public void setMedu(int medu) { this.medu = medu; }
    public int getFedu() { return fedu; }
    public void setFedu(int fedu) { this.fedu = fedu; }
    public String getMjob() { return mjob; }
    public void setMjob(String mjob) { this.mjob = mjob; }
    public String getFjob() { return fjob; }
    public void setFjob(String fjob) { this.fjob = fjob; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getGuardian() { return guardian; }
    public void setGuardian(String guardian) { this.guardian = guardian; }
    public int getTraveltime() { return traveltime; }
    public void setTraveltime(int traveltime) { this.traveltime = traveltime; }
    public int getStudytime() { return studytime; }
    public void setStudytime(int studytime) { this.studytime = studytime; }
    public int getFailures() { return failures; }
    public void setFailures(int failures) { this.failures = failures; }
    public String getSchoolsup() { return schoolsup; }
    public void setSchoolsup(String schoolsup) { this.schoolsup = schoolsup; }
    public String getFamsup() { return famsup; }
    public void setFamsup(String famsup) { this.famsup = famsup; }
    public String getPaid() { return paid; }
    public void setPaid(String paid) { this.paid = paid; }
    public String getActivities() { return activities; }
    public void setActivities(String activities) { this.activities = activities; }
    public String getNursery() { return nursery; }
    public void setNursery(String nursery) { this.nursery = nursery; }
    public String getHigher() { return higher; }
    public void setHigher(String higher) { this.higher = higher; }
    public String getInternet() { return internet; }
    public void setInternet(String internet) { this.internet = internet; }
    public String getRomantic() { return romantic; }
    public void setRomantic(String romantic) { this.romantic = romantic; }
    public int getFamrel() { return famrel; }
    public void setFamrel(int famrel) { this.famrel = famrel; }
    public int getFreetime() { return freetime; }
    public void setFreetime(int freetime) { this.freetime = freetime; }
    public int getGoout() { return goout; }
    public void setGoout(int goout) { this.goout = goout; }
    public int getDalc() { return dalc; }
    public void setDalc(int dalc) { this.dalc = dalc; }
    public int getWalc() { return walc; }
    public void setWalc(int walc) { this.walc = walc; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getAbsences() { return absences; }
    public void setAbsences(int absences) { this.absences = absences; }
    public int getG1() { return g1; }
    public void setG1(int g1) { this.g1 = g1; }
    public int getG2() { return g2; }
    public void setG2(int g2) { this.g2 = g2; }
    public int getG3() { return g3; }
    public void setG3(int g3) { this.g3 = g3; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}
