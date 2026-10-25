<img src="Report-3_Software_Requirement_Specification_media/media/image35.png" style="width:2.96778in;height:0.81186in" />

**<span class="smallcaps">CAPSTONE PROJECT REPORT</span>**

**Report 3 – Software Requirement Specification**

> – Hanoi, Aug 2026 –

**Table of Contents**

[Change Log 4](#change-log)

[Definition and Acronyms 5](#definition-and-acronyms)

[I. Overall Requirements 5](#i.-overall-requirements)

> [1. Context Diagram 5](#context-diagram)
>
> [2. Main Business Flow 5](#main-business-flow)
>
> [2.1 Candidate Self-Service (BF-01) 5](#_heading=h.jjao9lcm682g)
>
> [2.2 End-to-End Recruitment (BF-02) 5](#_heading=h.gykzhnm7zfnd)
>
> [2.3 Business Flow Name (Code) 6](#_heading=h.geacaoalf5x2)
>
> [3. Conceptual Data Model 6](#conceptual-data-model)
>
> [3.1 Entity Relationship Diagram 6](#entity-relationship-diagram)
>
> [3.2 Entity Descriptions 6](#entity-descriptions)
>
> [3.3 Data Business Rules 7](#data-business-rules)
>
> [4. User Requirements 8](#user-requirements)
>
> [4.1 Actors 8](#actors)
>
> [4.2 Use Cases (UC) 8](#use-cases-uc)
>
> [4.3 Use Case Diagrams 9](#section-21)
>
> [4.4 Permission Matrix 9](#permission-matrix)
>
> [5. System Functionalities 10](#system-functionalities)
>
> [5.1 Screen Inventory 10](#screen-inventory)
>
> [5.2 External API Inventory 10](#external-api-inventory)
>
> [5.3 Background Job Inventory 11](#background-job-inventory)

[II. Use Case Specifications 11](#ii.-use-case-specifications)

> [1. \<\<Feature Name1\>\> 11](#_heading=h.30u2ooeswjn)
>
> [1.1 UC Name1 11](#_heading=h.z1f2u8d34vyo)
>
> [1.2 Login System 12](#_heading=h.anr31hyfl6av)
>
> [1.3 UC Name2 12](#_heading=h.jf2j4517ycgw)
>
> [2. Xyz Feature 12](#_heading=h.mp07onz9vv92)

[III. Functional Requirements 13](#iii.-functional-requirements)

> [1. Feature Name1 13](#public-guest-features)
>
> [1.1 SubFeature Name1.1 13](#home-page-navigation)
>
> [1.2 SubFeature Name1.2 13](#_heading=h.14sfr943dt5k)
>
> [2. User Authentication 13](#learner-features)
>
> [2.1 User Register 13](#authentication-profile-management)
>
> [2.2 User Login 13](#_heading=h.wmkazcdgprpt)
>
> [3. System Administration 14](#section-187)
>
> [3.1 System Settings 14](#commerce-orders)
>
> [3.2 User Management 15](#_heading=h.j20ia6pyngp6)
>
> [4. Feature NameN 15](#_heading=h.ce67lmob8jsz)
>
> [5. Background Job Inventory 15](#background-job-inventory-1)
>
> [5.1 Unlock Expired Locked Accounts (JOB-01) 15](#send-automated-notifications-job-01)
>
> [5.2 Clean Up Old Audit Log Entries (JOB-02) 15](#update-learning-path-after-final-assessment-job-02)
>
> [6. External API Inventory 16](#external-api-inventory-1)
>
> [6.1 HTTP Method: URL pattern (API-ID) 16](#gemini-api-ai-learning-path-api-01)
>
> [6.2 VnPay Webhook (API-02) 16](#gemini-api-generate-jlpt-mock-test-api-02)
>
> [6.3 SePay Webhook (API-03) 16](#vnpay-payment-api-api-03)

[IV. Non-Functional Requirements 16](#iv.-non-functional-requirements)

> [1. External Interfaces 16](#external-interfaces)
>
> [2. Quality Attributes 16](#quality-attributes)
>
> [2.1 Usability 16](#usability)
>
> [2.2 Performance 17](#performance)
>
> [2.3 Security 17](#security)

[V. Requirement Appendix 17](#v.-requirement-appendix)

> [1. Business Rules 17](#business-rules)
>
> [2. System Messages 18](#system-messages)
>
> [3. Other Requirements 18](#other-requirements)

# Change Log

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 9%" />
<col style="width: 14%" />
<col style="width: 65%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Date</strong></th>
<th><strong>A*<br />
M, D</strong></th>
<th><strong>In charge</strong></th>
<th><strong>Change Description</strong></th>
</tr>
<tr class="odd">
<th>15/09/2026</th>
<th>A</th>
<th>HungNHHE186547</th>
<th><p>Main Business Flow (2.1, 2.2, 2.3, 2.4)</p>
<p>User Requirements (4.1, 4.2, 4.3.1)</p></th>
</tr>
<tr class="header">
<th>30/09/2026</th>
<th>A</th>
<th>HoangHH</th>
<th><p>Function Requirement (11,12,13,14)<br />
3. Conceptual Data Model(3.1 Entity Relationship Diagram)</p>
<h3 id="section"></h3></th>
</tr>
<tr class="odd">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

\*A - Added M - Modified D - Deleted

# Definition and Acronyms 

| **Acronym** | **Definition**            |
|-------------|---------------------------|
| COS         | Cafeteria Ordering System |
| …           |                           |

# I. Overall Requirements

## 1. Context Diagram

<img src="Report-3_Software_Requirement_Specification_media/media/image44.png" style="width:6.28438in;height:3.58333in" />

## 2. Main Business Flow

### 2.1 Take the assessment, receive a recommended study path, and register for the course. (BF-1) 

**Trigger:** The user accesses the system, creates an account, and successfully logs in to start the proficiency assessment.

**End Condition:** The learner completes the course and takes the final assessment. If the learner fails, the system creates a new learning path so the learner can continue studying, strengthen their knowledge, and retake the assessment.

<img src="Report-3_Software_Requirement_Specification_media/media/image1.png" style="width:6.28438in;height:1.33333in" />

### 2.2 JLPT Mock Exam Registration and Assessment (BF-2) 

**Trigger:** The learner logs into the system, opens the JLPT Mock Exam feature, selects a JLPT level and an available mock exam, and then starts taking the exam.

**End Condition:** The learner completes and successfully submits the mock exam. JEPS calculates and saves the exam results and allows the learner to view their score and review the correct answers. After that, AI analyzes the learner's strengths and weaknesses and recommends suitable courses for the learner to consider.

<img src="Report-3_Software_Requirement_Specification_media/media/image12.png" style="width:6.28438in;height:1.08333in" />

### 2.3 Course and Assessment Management (BF-3) 

**Trigger:** The Manager logs into JEPS and starts managing a course by opening the Course Management feature. The Manager then selects the course to edit its lessons, learning materials, practice exercises, and Milestone tests.

**End Condition:** The Manager successfully updates and saves the course after organizing lessons, adding learning materials, creating practice exercises, reviewing practice questions, creating Milestone tests, setting the question order, and configuring the exams. AI can be used to generate questions for practice exercises and Milestone tests. If AI is not used, the Manager can import questions from external sources or add them manually. After all content is completed and reviewed, JEPS successfully updates the course. If AI-generated or imported questions are incomplete or unsuitable, the Manager checks and edits them before saving the course.

<img src="Report-3_Software_Requirement_Specification_media/media/image42.png" style="width:6.28438in;height:0.93056in" />

## 3. Conceptual Data Model

### 3.1 Entity Relationship Diagram

<img src="Report-3_Software_Requirement_Specification_media/media/image46.jpg" style="width:6.28438in;height:4.375in" />

### 

### 

### 

### 

### 3.2 Entity Descriptions

**Entity: User**

| **Field**               | **Value**                                                                                            |
|-------------------------|------------------------------------------------------------------------------------------------------|
| Purpose                 | Stores account information of users in JEPS, including Learners, Managers, and Admins.               |
| Key business attributes | Full name, username, email address, password, role, account status                                   |
| Business identity       | Each User is uniquely identified in the system. Username and email are used to identify the account. |
| Status / lifecycle      | Users are managed based on their account status and can be active or disabled.                       |

**Entity: Course**

| **Field**               | **Value**                                                                                    |
|-------------------------|----------------------------------------------------------------------------------------------|
| Purpose                 | Stores information about JLPT courses provided to Learners.                                  |
| Key business attributes | Course title, JLPT level, price, description, status, duration, created date, published date |
| Business identity       | Each Course is uniquely identified by a Course ID.                                           |
| Status / lifecycle      | Courses are created, updated, and published based on their status.                           |

**Entity: Lesson**

| Field                   | Value                                                                                  |
|-------------------------|----------------------------------------------------------------------------------------|
| Purpose                 | Stores Lessons that belong to a Course.                                                |
| Key business attributes | Course, lesson title, order index, description, skill type, estimated duration, status |
| Business identity       | Each Lesson is uniquely identified and belongs to a Course.                            |
| Status / lifecycle      | Lessons are managed based on their status and learning order within a Course.          |

**Entity: Study Materials**

| **Field**               | **Value**                                                                                                             |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Purpose                 | Stores study materials used in each Lesson.                                                                           |
| Key business attributes | Lesson, material title, material type, description, body content, audio URL, file URL, JLPT level, skill type, status |
| Business identity       | Each Study Material is uniquely identified and belongs to a Lesson.                                                   |
| Status / lifecycle      | Study Materials are created, updated, and used when Learners study Lessons.                                           |

**Entity: Flashcard**

| **Field**               | **Value**                                                                                                      |
|-------------------------|----------------------------------------------------------------------------------------------------------------|
| Purpose                 | Stores Flashcards used to review content in a Lesson.                                                          |
| Key business attributes | Lesson, front content, back content, vocabulary, reading, meaning, example sentence, JLPT level, review status |
| Business identity       | Each Flashcard is uniquely identified and belongs to a Lesson.                                                 |
| Status / lifecycle      | Flashcards are used when Learners review lesson content, and their review status can be tracked.               |

**Entity: Exercise**

| **Field**               | **Value**                                                                                         |
|-------------------------|---------------------------------------------------------------------------------------------------|
| Purpose                 | Stores practice exercises that belong to Lessons for Learners to practice what they have learned. |
| Key business attributes | Lesson, exercise title, total questions, passing score                                            |
| Business identity       | Each Exercise is uniquely identified and belongs to a Lesson.                                     |
| Status / lifecycle      | Exercises are created and set up before Learners take them.                                       |

**Entity: Exercise Result**

| **Field**               | **Value**                                                                                     |
|-------------------------|-----------------------------------------------------------------------------------------------|
| Purpose                 | Stores the result of each Exercise attempt made by a Learner.                                 |
| Key business attributes | Learner, Exercise, attempt number, score, percentage, result status, submitted date, feedback |
| Business identity       | Each Exercise Result represents one attempt at an Exercise by a Learner.                      |
| Status / lifecycle      | Created when a Learner completes and submits an Exercise.                                     |

**Entity: Question Bank**

| **Field**               | **Value**                                                                                                            |
|-------------------------|----------------------------------------------------------------------------------------------------------------------|
| Purpose                 | Stores questions used for Exercises and Exams.                                                                       |
| Key business attributes | Question text, question type, category, JLPT level, difficulty level, correct answer, explanation, audio URL, status |
| Business identity       | Each Question is uniquely identified in the Question Bank.                                                           |
| Status / lifecycle      | Questions can be created, updated, imported, and used in Exercises or Exams.                                         |

**Entity: Exam**

| **Field**               | **Value**                                                                                                                        |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| Purpose                 | Stores information about JEPS exams, including Placement Assessment, Milestone Exam, and JLPT Exam, based on the current design. |
| Key business attributes | Course, exam title, exam type, JLPT level, duration, total questions, passing score, status                                      |
| Business identity       | Each Exam is uniquely identified by an Exam ID.                                                                                  |
| Status / lifecycle      | Exams are created, updated, and managed based on their status.                                                                   |

**Entity: Exam Result**

| **Field**               | **Value**                                                                                   |
|-------------------------|---------------------------------------------------------------------------------------------|
| Purpose                 | Stores Learners' results after taking Exams.                                                |
| Key business attributes | Learner, Exam, start time, end time, total score, percentage, passed status, result status  |
| Business identity       | Each Exam Result represents the result of one Exam attempt by a Learner.                    |
| Status / lifecycle      | Created when a Learner starts an Exam and completed after the Exam is submitted and graded. |

**Entity: Order**

| **Field**               | **Value**                                                                             |
|-------------------------|---------------------------------------------------------------------------------------|
| Purpose                 | Stores order information when Learners register for or purchase Courses.              |
| Key business attributes | Learner, Course, order date, total amount, order status, registration status          |
| Business identity       | Each Order represents one Course registration transaction made by a Learner.          |
| Status / lifecycle      | Orders are created during Checkout, and their status changes during order processing. |

**Entity: AI Recommend Course**

| **Field**               | **Value**                                                                                          |
|-------------------------|----------------------------------------------------------------------------------------------------|
| Purpose                 | Stores Course recommendations generated by AI for Learners.                                        |
| Key business attributes | Learner, recommended course, target JLPT level, recommendation reason, match score, generated date |
| Business identity       | Each AI Recommendation represents one Course recommendation for a Learner.                         |
| Status / lifecycle      | AI Course Recommendations are generated for Learners based on their learning needs and JLPT goals. |

**Entity: Learning Path**

| **Field**               | **Value**                                                                                                                                |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Purpose                 | Stores personalized Learning Paths for Learners.                                                                                         |
| Key business attributes | Daily hours setup, target level, current level, path status, scheduled date, learning goal, progress, status, created date, updated date |
| Business identity       | Each Learning Path represents one learning path for a Learner.                                                                           |
| Status / lifecycle      | Learning Paths are created after Learners set their learning goals and can be adjusted during the learning process.                      |

**Entity: Report**

| **Field**               | **Value**                                                                                                                   |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Purpose                 | Stores reports related to learning progress and results.                                                                    |
| Key business attributes | Report type, Learner, Course, generated date, learning progress, score summary, completion rate, strengths, recommendations |
| Business identity       | Each Report represents a report created from a Learner's learning data.                                                     |
| Status / lifecycle      | Reports are created when the system collects and summarizes Learners' learning data or results.                             |

### 

### 

### 3.3 Data Business Rules

## 

## 

| **Rule**                                                   | **Description**                                                                                                                                        | **Enforced On**                                                      |
|------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| Unique Username                                            | No two users can have the same username, regardless of their roles.                                                                                    | Account creation and username update                                 |
| Unique Email                                               | No two users can use the same email address.                                                                                                           | Account registration and email update                                |
| Valid Account Information                                  | Full name, email, phone number, and password must meet the required validation rules before an account is created.                                     | Account registration                                                 |
| Password Security                                          | User passwords must be hashed before being stored in the database. Passwords must not be stored as plain text.                                         | Account registration and password change                             |
| Active Account Required                                    | Users with disabled accounts cannot log in or access system functions that require authentication.                                                     | User login and account status update                                 |
| Valid JLPT Level                                           | Course, exam, question, and learning goal levels must be N5, N4, N3, N2, or N1.                                                                        | Course, exam, question, and learning goal creation or update         |
| Valid Course Information                                   | Course prices must be greater than or equal to 0, and course duration must be greater than 0.                                                          | Course creation and update                                           |
| Valid Lesson Order                                         | Lesson order values must be positive integers and must not have the same position within a course when unique ordering is required.                    | Lesson creation and ordering                                         |
| Valid Study Materials                                      | Study materials must contain all required valid information and use supported file formats or reference links.                                         | Study material creation and update                                   |
| Valid Question Data                                        | Each question must have valid content, a supported question type, and a correct answer that matches the available answer options.                      | Question creation, update, and import                                |
| Valid Practice Quiz Settings                               | Practice quizzes must contain valid questions and have a passing score within the allowed score range.                                                 | Practice quiz creation and update                                    |
| AI-Generated Content Must Be Reviewed                      | Practice quizzes and exams generated by AI must be reviewed and approved by the Manager before being saved for use.                                    | Practice quiz and exam generation                                    |
| Valid Exam Settings                                        | Exam duration and maximum score must be greater than 0, while the passing score must be between 0 and the maximum score.                               | Exam configuration                                                   |
| Exams Must Have Valid Questions                            | An exam must have a valid set of questions before its configuration is completed and it can be used.                                                   | Exam creation and configuration                                      |
| Placement Assessment Requires Login                        | Only Learners who have logged in and been authenticated can take the Placement Assessment.                                                             | Starting the Placement Assessment                                    |
| Placement Assessment Level Selection                       | The Placement Assessment must be selected based on the target JLPT level and the previous learning level provided by the Learner.                      | Placement Assessment selection                                       |
| Submitted Answers Must Be Valid                            | Submitted answers must follow the required structure and match valid questions or answer options.                                                      | Assessment and exam submission                                       |
| Exam Results Require Completed Submissions                 | Exam results can only be created from exam attempts that have been successfully submitted, processed, and graded.                                      | Exam result creation                                                 |
| Exam Results Belong to the Learner                         | Learners can only view exam results and solutions related to their own exam attempts.                                                                  | Exam result access                                                   |
| Course Recommendations Require Assessment Data             | AI personalized course recommendations require valid assessment results or related learning data.                                                      | AI course recommendation generation                                  |
| Course Enrollment Requires a Valid Course                  | Learners can only enroll in courses that exist and are still available for enrollment.                                                                 | Course enrollment and payment                                        |
| Prevent Duplicate Course Enrollment                        | Learners cannot purchase a course they already own unless the enrollment rules allow them to purchase it again.                                        | Order checking before payment                                        |
| Payment Requires a Valid Order                             | A payment transaction must be linked to a valid order that is waiting for payment and has a valid payment amount.                                      | Payment creation                                                     |
| Payment Verification Required                              | Successful payments must be confirmed by checking the response from the payment gateway, transaction status, order reference code, and payment amount. | Payment confirmation                                                 |
| Prevent Duplicate Payment Processing                       | Duplicate payment notifications must not cause repeated payment processing or activate course access more than once.                                   | Payment response processing                                          |
| Course Access Requires Successful Payment                  | Access to paid courses can only be activated after successful payment confirmation.                                                                    | Course enrollment activation and lesson access                       |
| Learning Path Requires Valid Conditions                    | AI learning path generation requires assessment results, active course access, and completed learning goal and study time setup.                       | Learning path generation                                             |
| Valid Daily Study Time                                     | Daily study time must be a number greater than 0 and must not exceed 24 hours.                                                                         | Learning goal and study time setup                                   |
| Learning Path and Course Consistency                       | A generated learning path must not contain lessons that do not belong to the related course.                                                           | Learning path creation and update                                    |
| Learning Progress Ownership                                | Learners can only track their own learning paths and learning progress.                                                                                | Learning path progress access                                        |
| Lesson Access Restrictions                                 | Learners can only access lessons and learning activities from courses they have valid access to.                                                       | Access to lessons, Flashcards, study materials, and practice quizzes |
| Milestone Exam Requirements                                | Learners can only take a Milestone Exam when they meet the required learning conditions and the exam is available.                                     | Starting a Milestone Exam                                            |
| Milestone Exam Result Status                               | The Pass or Fail status of a Milestone Exam must be determined by comparing the achieved score with the configured passing score.                      | Milestone Exam grading                                               |
| Failing a Milestone Exam Triggers Learning Path Adjustment | When a Learner fails a Milestone Exam, the system triggers a learning path adjustment to help the Learner review weak areas.                           | Milestone Exam result processing                                     |
| Passing a Milestone Exam Does Not Trigger Review           | When a Learner passes a Milestone Exam, the system does not trigger the learning path adjustment process used for failed exams.                        | Milestone Exam result processing                                     |
| Learning Path Update Validation                            | An updated learning path must contain valid lessons and a valid study schedule before replacing the previously saved version.                          | Learning path adjustment                                             |
| JLPT Mock Exam Requires a Valid Level                      | The JLPT Mock Exam must match the JLPT level selected by the Learner.                                                                                  | JLPT Mock Exam selection and creation                                |
| Exam Availability Restrictions                             | Learners cannot start exams that are not available or have not been properly configured.                                                               | Starting an exam                                                     |
| Exam Time Limit Enforcement                                | Exam submission must follow the configured time limits and rules for handling expired exam time.                                                       | Taking and submitting an exam                                        |
| Invoice Requires Successful Payment                        | Invoice information is recorded after successful payment confirmation and must match the completed transaction.                                        | Invoice creation and access                                          |
| Order History Ownership                                    | Learners can only view orders and order details related to their own accounts.                                                                         | Order history and order details access                               |

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 4. User Requirements

### 4.1 Actors

<table>
<colgroup>
<col style="width: 4%" />
<col style="width: 19%" />
<col style="width: 76%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>#</strong></th>
<th><strong>Actor</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>1</p>
</blockquote></th>
<th>Guest</th>
<th>The user is not logged in. They can register for an account, view the homepage, read the introduction, view course list, view course details, and view JLPT Mock exams.</th>
</tr>
<tr class="header">
<th><blockquote>
<p>2</p>
</blockquote></th>
<th>Learner</th>
<th>Logged-in users can view and edit their profiles, change passwords, view current learning paths, take placement assignment, view results, see AI-suggested learning paths, take courses, practice, take JLPT mock exams, make payments, and view order history.</th>
</tr>
<tr class="odd">
<th><blockquote>
<p>3</p>
</blockquote></th>
<th>Manager</th>
<th>Responsible for managing learning content and academic activities. Capable of managing announcements, knowledge data, lessons, courses, question banks, assignments, assessments, learner lists, JLPT mock exams, and reports.</th>
</tr>
<tr class="header">
<th><blockquote>
<p>4</p>
</blockquote></th>
<th>Admin</th>
<th>The administrator of the entire system. They can manage users, roles, exam system configurations, exam rules, and exam settings, as well as monitor system logs.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

### 

### 

### 

### 4.2 Use Cases (UC)

| **ID** | **Use Case**                         | **Feature**                   | **Use Case Description**                                                                     |
|--------|--------------------------------------|-------------------------------|----------------------------------------------------------------------------------------------|
| UC-01  | Register Account                     | Account Management            | Guests create a new account to use the learning features of JEPS.                            |
| UC-02  | View Home Page                       | General Information           | View the home page and general information about the system.                                 |
| UC-03  | View About Us                        | General Information           | View information about the introduction, objectives, and direction of JEPS.                  |
| UC-04  | View Course List                     | Course                        | View the list of courses currently available in the system.                                  |
| UC-05  | View Course Details                  | Course                        | View detailed information about a selected course.                                           |
| UC-06  | View JLPT Mock Exam                  | JLPT Mock Exam                | View general information about the JLPT mock exam feature of JEPS.                           |
| UC-07  | Login                                | Account Management            | Log in to access personal and learning features.                                             |
| UC-08  | Reset Password                       | Account Management            | Reset the password when the current password cannot be used.                                 |
| UC-09  | View Profile                         | Profile Management            | View personal information and account information.                                           |
| UC-10  | Edit Profile                         | Profile Management            | Edit personal information in the profile.                                                    |
| UC-11  | Change Password                      | Profile Management            | Change the account password.                                                                 |
| UC-12  | Learner Dashboard                    | Dashboard                     | View an overview of the learning process on JEPS.                                            |
| UC-13  | Take Placement Assessment Exam       | Placement Assessment          | Take a placement assessment to determine the current proficiency level.                      |
| UC-14  | Select Prior Study Level             | Placement Assessment          | Select the previously studied level before taking the assessment.                            |
| UC-15  | Declare Prior Level                  | Placement Assessment          | Declare the previous Japanese proficiency level to support the assessment process.           |
| UC-16  | View Exam Result & Solutions         | Exam Result                   | View scores, results, and answers after completing the exam.                                 |
| UC-17  | Generate AI Personalized Course      | AI Course Recommendation      | The system uses learning results to generate personalized course recommendations.            |
| UC-18  | View Recommended Course              | Course Recommendation         | View courses recommended by the system based on assessment results.                          |
| UC-19  | Register for Course                  | Course Registration           | Register for a selected or recommended course.                                               |
| UC-20  | Checkout                             | Order & Payment               | Review course and order information before making payment.                                   |
| UC-21  | Make Payment                         | Payment                       | Pay for the order through a supported payment gateway.                                       |
| UC-22  | View Invoice                         | Order & Payment               | View the invoice of a completed payment transaction.                                         |
| UC-23  | Track Order History                  | Order Management              | View the history of created orders.                                                          |
| UC-24  | View Orders Detail                   | Order Management              | View detailed information about a selected order.                                            |
| UC-25  | Generate AI Learning Path            | Learning Path                 | The system generates a personalized learning path based on learning goals and learning data. |
| UC-26  | Hours Setup                          | Learning Path                 | Set learning goals and study hours so that the system can generate a suitable learning path. |
| UC-27  | Track Learning Path Progress         | Learning Path                 | Track the progress of completing the learning path.                                          |
| UC-28  | Recalibrate Learning Path Schedule   | Learning Path                 | Adjust the study schedule in the learning path when necessary.                               |
| UC-29  | Study Lesson                         | Learning                      | Access and study the content of a lesson.                                                    |
| UC-30  | Study Materials                      | Learning                      | View and study the materials provided in the lesson.                                         |
| UC-31  | Practice Lesson Flashcard            | Flashcard                     | Review lesson content through flashcards.                                                    |
| UC-32  | Do Exercise Quiz                     | Exercise                      | Complete lesson exercises or quizzes.                                                        |
| UC-33  | Take Milestone Exam                  | Assessment                    | Take a milestone exam during the learning process.                                           |
| UC-34  | Take JLPT Mock Exam                  | JLPT Mock Exam                | Take a JLPT mock exam to assess readiness.                                                   |
| UC-35  | Track Exam History                   | Exam History                  | View the history of completed assessments and exams.                                         |
| UC-36  | View Manager Dashboard               | Manager Dashboard             | View an overview of the operational status and content of JEPS.                              |
| UC-37  | Manage Course                        | Course Management             | Manage courses in the system.                                                                |
| UC-38  | Set up Price                         | Course Management             | Set or update course prices.                                                                 |
| UC-39  | Manage Learning Lesson               | Lesson Management             | Manage lessons belonging to courses.                                                         |
| UC-40  | Create Learning Lesson               | Lesson Management             | Create a new lesson for a course.                                                            |
| UC-41  | Update Learning Lesson               | Lesson Management             | Update the content of an existing lesson.                                                    |
| UC-42  | Organize Learning Lesson             | Lesson Management             | Arrange the order and structure of lessons in a course.                                      |
| UC-43  | Manage Question Bank                 | Question Bank Management      | Manage the question bank used for exercises and exams.                                       |
| UC-44  | Update Question                      | Question Bank Management      | Edit existing questions in the question bank.                                                |
| UC-45  | Import Questions                     | Question Bank Management      | Import questions from external sources into the system.                                      |
| UC-46  | Manage Exercise                      | Exercise Management           | Manage exercises belonging to courses or lessons.                                            |
| UC-47  | Create Exercise                      | Exercise Management           | Create a new exercise.                                                                       |
| UC-48  | Update Exercise                      | Exercise Management           | Edit an existing exercise.                                                                   |
| UC-49  | Generate Exercise                    | Exercise Management           | The system supports generating exercises based on existing data or questions.                |
| UC-50  | Manage Exam                          | Exam Management               | Manage Placement Assessment, Milestone Exam, and JLPT Mock Exam.                             |
| UC-51  | Create Exam                          | Exam Management               | Create a new exam or assessment.                                                             |
| UC-52  | Generate Exam                        | Exam Management               | The system supports generating exams based on the existing question bank.                    |
| UC-53  | Import Exam                          | Exam Management               | Import exams from external sources into JEPS.                                                |
| UC-54  | Update Exam                          | Exam Management               | Edit exam information and content.                                                           |
| UC-55  | Config Exam and Exercise             | Exam & Exercise Configuration | Configure parameters applied to exams and exercises.                                         |
| UC-56  | Set up Exam and Exercise Information | Exam & Exercise Configuration | Set up basic information for exams and exercises.                                            |
| UC-57  | Set Up Time And Score                | Exam & Exercise Configuration | Set up the time limit and scores.                                                            |
| UC-58  | Change Test Status                   | Exam & Exercise Configuration | Change the operational status of an exam or exercise.                                        |
| UC-59  | View Reports                         | Report Management             | View reports provided by JEPS.                                                               |
| UC-60  | Export Reports                       | Report Management             | Export reports to files for storage or external use.                                         |
| UC-61  | View Admin Dashboard                 | Administration                | View an overview of the system status.                                                       |
| UC-62  | View User                            | User Management               | View the list of users in JEPS.                                                              |
| UC-63  | View User Detail                     | User Management               | View detailed information about a user.                                                      |
| UC-64  | Update User                          | User Management               | Update user information or status.                                                           |
| UC-65  | Monitor System Log                   | System Log Management         | Monitor activity logs and system events.                                                     |

###  

#### 4.3.1 UCs for Guest

<img src="Report-3_Software_Requirement_Specification_media/media/image43.png" style="width:6.28438in;height:3.65278in" />

#### 4.3.2 UCs for Learner

<img src="Report-3_Software_Requirement_Specification_media/media/image20.png" style="width:6.28438in;height:3.90278in" />

#### 4.3.3 UCs for Manager

<img src="Report-3_Software_Requirement_Specification_media/media/image27.png" style="width:6.28438in;height:4.76389in" />

#### 

#### 4.3.4 UCs for Admin

<img src="Report-3_Software_Requirement_Specification_media/media/image2.png" style="width:6.28438in;height:3.58333in" />

### 4.4 Permission Matrix

Full = unrestricted · Restricted = conditions apply (see footnotes) · No = denied

| **Entity / Function**   | **Action**                           | **Admin**   | **Manager** | **Learner**  | **Guest**   |
|-------------------------|--------------------------------------|-------------|-------------|--------------|-------------|
| User Account            | Register Account                     | No          | No          | No           | Full        |
| User Account            | Login                                | Full        | Full        | Full         | No          |
| Password                | Reset Password                       | Restricted¹ | Restricted¹ | Restricted¹  | Restricted¹ |
| Profile                 | View Profile                         | Restricted² | Restricted² | Restricted²  | No          |
| Profile                 | Edit Profile                         | Restricted² | Restricted² | Restricted²  | No          |
| Password                | Change Password                      | Restricted² | Restricted² | Restricted²  | No          |
| Home Page               | View Home Page                       | Full        | Full        | Full         | Full        |
| About Us                | View About Us                        | Full        | Full        | Full         | Full        |
| Course                  | View Course List                     | Full        | Full        | Full         | Full        |
| Course                  | View Course Details                  | Full        | Full        | Full         | Full        |
| JLPT Mock Exam          | View JLPT Mock Exam                  | Full        | Full        | Full         | Full        |
| Learner Dashboard       | View Dashboard                       | No          | No          | Full         | No          |
| Placement Assessment    | Take Placement Assessment Exam       | No          | No          | Restricted³  | No          |
| Prior Study Level       | Select Prior Study Level             | No          | No          | Restricted³  | No          |
| Prior Level             | Declare Prior Level                  | No          | No          | Restricted³  | No          |
| Exam Result & Solutions | View Exam Result & Solutions         | No          | No          | Restricted⁴  | No          |
| AI Personalized Course  | Generate AI Personalized Course      | No          | No          | Restricted⁵  | No          |
| Recommended Course      | View Recommended Course              | No          | No          | Restricted⁵  | No          |
| Course Registration     | Register for Course                  | No          | No          | Restricted⁶  | No          |
| Checkout                | Checkout                             | No          | No          | Restricted⁶  | No          |
| Payment                 | Make Payment                         | No          | No          | Restricted⁶  | No          |
| Invoice                 | View Invoice                         | No          | No          | Restricted⁷  | No          |
| Order History           | Track Order History                  | No          | No          | Restricted⁴  | No          |
| Order Details           | View Orders Detail                   | No          | No          | Restricted⁴  | No          |
| AI Learning Path        | Generate AI Learning Path            | No          | No          | Restricted⁸  | No          |
| Learning Goal & Hours   | Select Learning Goal & Hours Setup   | No          | No          | Restricted⁸  | No          |
| Learning Path           | Track Learning Path Progress         | No          | No          | Restricted⁹  | No          |
| Learning Path Schedule  | Recalibrate Learning Path Schedule   | No          | No          | Restricted⁹  | No          |
| Lesson                  | Study Lesson                         | No          | No          | Restricted¹⁰ | No          |
| Study Materials         | Study Materials                      | No          | No          | Restricted¹⁰ | No          |
| Flashcard               | Practice Lesson Flashcard            | No          | No          | Restricted¹⁰ | No          |
| Exercise Quiz           | Do Exercise Quiz                     | No          | No          | Restricted¹⁰ | No          |
| Milestone Exam          | Take Milestone Exam                  | No          | No          | Restricted¹¹ | No          |
| JLPT Mock Exam          | Take JLPT Mock Exam                  | No          | No          | Restricted¹² | No          |
| Exam History            | Track Exam History                   | No          | No          | Restricted⁴  | No          |
| Manager Dashboard       | View Manager Dashboard               | No          | Full        | No           | No          |
| Course                  | Manage Course                        | No          | Full        | No           | No          |
| Course                  | Set up Price                         | No          | Full        | No           | No          |
| Learning Lesson         | Manage Learning Lesson               | No          | Full        | No           | No          |
| Learning Lesson         | Create Learning Lesson               | No          | Full        | No           | No          |
| Learning Lesson         | Update Learning Lesson               | No          | Full        | No           | No          |
| Learning Lesson         | Organize Learning Lesson             | No          | Full        | No           | No          |
| Question Bank           | Manage Question Bank                 | No          | Full        | No           | No          |
| Question Bank           | Update Question                      | No          | Full        | No           | No          |
| Question Bank           | Import Questions                     | No          | Full        | No           | No          |
| Exercise                | Manage Exercise                      | No          | Full        | No           | No          |
| Exercise                | Create Exercise                      | No          | Full        | No           | No          |
| Exercise                | Update Exercise                      | No          | Full        | No           | No          |
| Exercise                | Generate Exercise                    | No          | Full        | No           | No          |
| Exam                    | Manage Exam                          | No          | Full        | No           | No          |
| Exam                    | Create Exam                          | No          | Full        | No           | No          |
| Exam                    | Generate Exam                        | No          | Full        | No           | No          |
| Exam                    | Import Exam                          | No          | Full        | No           | No          |
| Exam                    | Update Exam                          | No          | Full        | No           | No          |
| Exam & Exercise         | Config Exam and Exercise             | No          | Full        | No           | No          |
| Exam & Exercise         | Set up Exam and Exercise Information | No          | Full        | No           | No          |
| Exam & Exercise         | Set Up Time And Score                | No          | Full        | No           | No          |
| Exam & Exercise         | Change Test Status                   | No          | Full        | No           | No          |
| Report                  | View Reports                         | No          | Full        | No           | No          |
| Report                  | Export Reports                       | No          | Full        | No           | No          |
| Admin Dashboard         | View Admin Dashboard                 | Full        | No          | No           | No          |
| User Management         | View User                            | Full        | No          | No           | No          |
| User Management         | View User Detail                     | Full        | No          | No           | No          |

## 5. System Functionalities

### 5.1 Screen Inventory

#### a. Screen Flow

<img src="Report-3_Software_Requirement_Specification_media/media/image40.png" style="width:6.28438in;height:3.83333in" />

#### b. Screen List

| **\#** | **Screen Name**                      | **Feature**           | **Description**                                                                                         |
|--------|--------------------------------------|-----------------------|---------------------------------------------------------------------------------------------------------|
| 1      | Home Page                            | Navigation            | The main page of JEPS, providing navigation to the system's public features.                            |
| 2      | About Us                             | General Information   | Displays information about JEPS, its goals, and direction.                                              |
| 3      | Course List                          | Course                | Displays a list of available courses in the system.                                                     |
| 4      | Course Detail                        | Course                | Displays detailed information about the selected course.                                                |
| 5      | User Register                        | Authentication        | Allows Guests to create a new account.                                                                  |
| 6      | User Login                           | Authentication        | Allows users to log in to JEPS.                                                                         |
| 7      | Password Reset                       | Authentication        | Allows users to reset their password if they forget it.                                                 |
| 8      | User Profile                         | Profile Management    | Displays the user's personal and account information.                                                   |
| 9      | Password Change                      | Profile Management    | Allows users to change their current password.                                                          |
| 10     | Learner Dashboard                    | Learner Dashboard     | Displays an overview and features available to Learners.                                                |
| 11     | Place Assessment Exam                | Assessment            | Allows Learners to take an initial assessment test.                                                     |
| 12     | JLPT Target and Study Check          | Assessment            | Allows Learners to enter or confirm their previous learning level.                                      |
| 13     | JLPT Mock Exam                       | Examination           | Allows Learners to access the JLPT Mock Exam.                                                           |
| 14     | Milestone Exam                       | Examination           | Allows Learners to take tests at different stages of their learning process.                            |
| 15     | Exam Room                            | Examination           | Provides an interface for Learners to take exams.                                                       |
| 16     | Exam Result & Solutions              | Exam Result           | Displays scores, results, and solutions after completing an exam.                                       |
| 17     | Course                               | Course Learning       | Displays courses that Learners are currently studying or have access to.                                |
| 18     | Learning Process                     | Learning Process      | Displays course progress, learning paths, and lesson details.                                           |
| 19     | Lesson Detail                        | Learning              | Displays the content and activities of a lesson.                                                        |
| 20     | Flashcard                            | Learning              | Allows Learners to review lesson content using Flashcards.                                              |
| 21     | Study Materials                      | Learning Content      | Displays learning materials included in a Lesson.                                                       |
| 22     | Exercise Quiz                        | Exercise              | Allows Learners to complete practice exercises within a Lesson.                                         |
| 23     | Hours Setup                          | Learning Path         | Allows Learners to set learning goals and study time.                                                   |
| 24     | AI Learning Path Generation          | Learning Path         | Displays a personalized learning path generated by AI.                                                  |
| 25     | Learning Path Schedule Recalibration | Learning Path         | Adjusts the Learning Path schedule based on learning progress or new Milestone Exam results.            |
| 26     | AI Personalized Course Generation    | Course Recommendation | Displays courses recommended by the system for Learners.                                                |
| 27     | Order History                        | Order Management      | Displays the Learner's order history.                                                                   |
| 28     | Exam History                         | Examination           | Displays a list of exams completed by the Learner.                                                      |
| 29     | Checkout                             | Order Management      | Displays and confirms order information before payment.                                                 |
| 30     | Payment                              | Payment               | Allows Learners to pay for orders using payment methods supported by JEPS.                              |
| 31     | Invoice                              | Payment               | Displays invoice information after a successful payment.                                                |
| 32     | Post Login                           | Authentication        | Redirects users to the appropriate Dashboard based on their role after a successful login.              |
| 33     | Admin Dashboard                      | Administration        | Displays a system overview and provides access to administration features.                              |
| 34     | User Management                      | User Management       | Allows Admins to view, search, update account status, and manage user accounts.                         |
| 35     | User Details                         | User Management       | Displays detailed information about a user's account, role, and status.                                 |
| 36     | System Audit Log                     | System Monitoring     | Displays the history of activities and events recorded in the system.                                   |
| 37     | Manager Dashboard                    | Management            | Displays an overview and provides access to features for managing learning content, courses, and exams. |
| 38     | Course Management                    | Course Management     | Allows Managers to create, view, update, and manage courses.                                            |
| 39     | Learning Lesson Management           | Lesson Management     | Allows Managers to manage Lessons within courses.                                                       |
| 40     | Learning Lesson Organization         | Lesson Organization   | Allows Managers to organize and arrange Lessons and learn content structure.                            |
| 41     | Exercise Management                  | Exercise Management   | Allows Managers to create, edit, and manage practice exercises.                                         |
| 42     | Exercise Generation                  | Exercise Generation   | Provides an interface for creating practice exercises and setting up question content.                  |
| 43     | Exam Management                      | Exam Management       | Allows Managers to manage Milestone Exams, JLPT Mock Exams, and Placement Assessments.                  |
| 44     | Exam Generation                      | Exam Generation       | Allows Managers to create and set up exams using questions from the question bank.                      |
| 45     | Question Bank Management             | Question Bank         | Allows Managers to create, update, and manage the question bank.                                        |
| 46     | Report                               | Reporting             | Displays reports and statistics related to learning activities, courses, and exams.                     |

### 5.2 External API Inventory

| **\#** | **API Name**          | **Calling Path**         | **Description**                                                                                                          |
|--------|-----------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------------|
| 1      | Payment Service API   | JEPS → Payment Service   | Allows JEPS to process course payments and receive payment transaction results.                                          |
| 2      | Google Service API    | JEPS → Google Service    | Allows JEPS to send account registration, payment confirmation, course enrollment, and notification emails to Learners.  |
| 3      | AI Gemini Service API | JEPS → AI Gemini Service | Allows JEPS to analyze assessment results and generate personalized learning paths and recommended courses for Learners. |

### 5.3 Background Job Inventory

| **\#** | **Job Name**                                        | **Group**     | **Description**                                                                                                                                               |
|--------|-----------------------------------------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1      | Update Course Progress                              | Scheduled     | Runs periodically to summarize the completion status of lessons, practices, and assessments, then updates the Learner's course progress percentage.           |
| 2      | Update Learning Path After Final Assessment         | Async / Queue | After the Learner completes the Final Assessment, the system adds a request to the queue for AI to analyze the results and update the Learning Path.          |
| 3      | Generate Learning Path After Placement Assessment   | Async / Queue | After the system grades the Placement Assessment, the background job sends the necessary data to AI to generate a personalized Learning Path for the Learner. |
| 4      | Send Email or Notification After Successful Payment | Async / Queue | After receiving a successful payment result, the system sends an order confirmation notification and course information to the Learner.                       |

# 

# 

# 

# 

# 

# 

# 

# II. Use Case Specifications

## 1. Account Management

### 1.1 Register Account

### 

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 31%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Guest</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Guest registers a new account to access JEPS.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Guest is not logged in; registration is available.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Guest selects Register Account<br />
2. The system displays Full Name, Email, Phone Number, Password, and Confirm Password<br />
3. Guest enters the information and selects Register<br />
4. The system checks the format and duplicate information<br />
5. The system hashes the password and creates a Learner account<br />
6. The system shows a success message and redirects to Login</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Empty Fields:</strong> Required information is missing; show “This field is required.”<br />
<strong>A2. Invalid Name:</strong> The name is empty, contains only spaces, or is not 2–100 characters long; ask the user to enter it again<br />
<strong>A3. Invalid Email:</strong> The email is missing @ or the domain, or contains spaces; show an invalid email message<br />
<strong>A4. Invalid Phone:</strong> The phone number is not exactly 10 digits (0–9) starting with 0; ask the user to enter it again<br />
<strong>A5. Invalid Password:</strong> The password does not have 8–64 characters, uppercase and lowercase letters, numbers, and special characters; ask the user to change it<br />
<strong>A6. Password Mismatch:</strong> Confirm Password does not match Password<br />
<strong>A7. Duplicate Email / Phone:</strong> The email already exists, or the phone number already exists if it must be unique<br />
<strong>A8. Registration Failed:</strong> Data could not be saved; no account is created</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Learner account is saved; Guest can log in.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

### 

### 1.2 Login

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner, Manager, Admin</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">User logs in and accesses the Dashboard for their role.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">The user has a valid account and is not logged in.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. User opens Login<br />
2. The system displays Email and Password<br />
3. User enters the information and selects Login<br />
4. The system checks the information, account status, and role<br />
5. The system creates a login session<br />
6. The system redirects the user to the correct Dashboard</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Empty Email / Password:</strong> Both fields must be filled in<br />
<strong>A2. Invalid Email:</strong> The email format is incorrect<br />
<strong>A3. Wrong Credentials:</strong> The email or password is incorrect<br />
<strong>A4. Account Disabled:</strong> The account is disabled; login is denied<br />
<strong>A5. Authentication Error:</strong> Login cannot be processed; the user can try again</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">A login session is created; the user enters the correct Dashboard.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 2. Placement Assessment

### 2.1 Select Prior Study Level

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Select a JLPT goal and provide the previous study level to find a suitable assessment.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in; JLPT levels and Assessment tests are available.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner opens Placement Assessment<br />
2. The system displays JLPT N5–N1<br />
3. Learner selects a JLPT goal<br />
4. The system asks about the previous study level<br />
5. Learner enters a level or selects that they have never studied<br />
6. The system selects and loads the matching Assessment</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Target:</strong> No JLPT level from N5–N1 has been selected<br />
<strong>A2. No Previous Study:</strong> Select an assessment based on the rule for beginners<br />
<strong>A3. Previous Level:</strong> Select an assessment suitable for the entered level<br />
<strong>A4. Invalid Combination:</strong> The goal and previous level do not match; ask the Learner to select again<br />
<strong>A5. Unavailable Assessment:</strong> No matching assessment is available</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">1. The target level and assessment are determined.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 2.2 Take Placement Assessment

### 

<table style="width:100%;">
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Take an initial assessment to support skill analysis and course recommendations.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in and has selected an available Placement Assessment.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner selects Take Placement Assessment<br />
2. The system displays the content and questions<br />
3. Learner answers the questions and selects Submit Assessment<br />
4. The system checks and saves the answers<br />
5. The system scores the assessment using the answers and scoring rules<br />
6. The system saves the result and sends the data to AI for analysis<br />
7. Learner is redirected to the result</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Unanswered Required Questions:</strong> Required questions are unanswered; ask the Learner to complete them<br />
<strong>A2. Invalid Answer:</strong> An answer has the wrong format or is not one of the available choices<br />
<strong>A3. Submission Failed:</strong> The assessment could not be submitted; allow another attempt<br />
<strong>A4. Scoring Failed:</strong> The result is not ready<br />
<strong>A5. AI Failed:</strong> Keep the scored result and show that the analysis is not ready</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The answers and score are saved; the data can be used by AI.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 2.3 View Assessment Result

### 

<table style="width:100%;">
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">View the score and analysis of strengths and weaknesses after the Placement Assessment.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">The Assessment has been submitted and the system has a result.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner selects View Assessment Result<br />
2. The system gets the result and displays the score<br />
3. The system gets the AI analysis<br />
4. The system displays strengths, weaknesses, and areas to improve<br />
5. Learner views the result and goes to the recommended course</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Not Completed:</strong> The Assessment has not been taken<br />
<strong>A2. Result Pending:</strong> The result has not been processed yet<br />
<strong>A3. AI Analysis Unavailable:</strong> Still show the score and a message to wait for the analysis<br />
<strong>A4. Retrieval Error:</strong> Data could not be loaded; allow another attempt</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">Learner views the result; the data is used for course recommendations.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 3. AI Course Recommendation

### 3.1 View Recommended Course

### 

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">View courses or course packages recommended by AI based on the Placement Assessment or JLPT Mock Exam.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Assessment results and skill data are available; the course list is available.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner views the assessment or mock exam result<br />
2. The system gets the skill analysis and asks AI for recommendations if needed<br />
3. AI returns suitable courses<br />
4. The system checks them against the available courses<br />
5. The system displays the courses and reasons for the recommendations<br />
6. Learner chooses to view details or enroll</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Result:</strong> A suitable assessment result is missing<br />
<strong>A2. AI Failed:</strong> Recommendations are not available<br />
<strong>A3. No Matching Course:</strong> No suitable course is available<br />
<strong>A4. Course Unavailable:</strong> Remove courses that are no longer offered from the enrollment choices</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The recommended course list is displayed; Learner can proceed to Checkout.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 

## 

## 

## 4. Course Enrollment & Payment

### 4.1 Checkout

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Review and confirm an order before payment.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in and has selected an available course.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner selects a course<br />
2. The system displays Checkout with the price and total amount<br />
3. Learner reviews and confirms<br />
4. The system checks the order and course status<br />
5. The system creates an order waiting for payment<br />
6. The system moves to Payment</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Course:</strong> No course has been selected<br />
<strong>A2. Unavailable Course:</strong> The course is no longer sold<br />
<strong>A3. Invalid Amount:</strong> The amount is invalid or does not match<br />
<strong>A4. Duplicate Enrollment:</strong> Learner already owns the course and is not allowed to buy it again<br />
<strong>A5. Cancelled Checkout:</strong> Return without making a payment</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">An order waiting for payment is created.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 4.2 Make Payment

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>VNPay Payment Gateway</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Pay for an order through VNPay to activate course access.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">There is an order waiting for payment and a valid payment request.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner selects Make Payment<br />
2. The system creates a transaction and redirects to VNPay<br />
3. Learner pays on VNPay<br />
4. VNPay returns the transaction result<br />
5. The system checks the signature, order code, amount, and status<br />
6. The system updates the payment and activates course access<br />
7. The system saves the transaction and invoice and shows the result</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Cancelled:</strong> Learner cancels the transaction<br />
<strong>A2. Failed:</strong> VNPay returns a failed result<br />
<strong>A3. Timeout:</strong> No confirmation has been received; keep the waiting status<br />
<strong>A4. Invalid Signature:</strong> Reject an invalid response<br />
<strong>A5. Amount Mismatch:</strong> The amount does not match; do not activate course access<br />
<strong>A6. Duplicate Notification:</strong> Do not process the same payment twice<br />
<strong>A7. Gateway Error:</strong> Cannot connect to VNPay</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">If successful, the order is paid and course access is activated.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 

## 

## 

## 

## 5. AI Learning Path

### 5.1 Select Learning Goal & Hours Setup

### 

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Set a JLPT goal and daily study hours.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in, has successfully paid, and has an Assessment result.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner opens Hours Setup<br />
2. The system displays the JLPT goal<br />
3. Learner enters the number of study hours per day<br />
4. Learner confirms<br />
5. The system checks and saves the information<br />
6. The system sends the data to Generate AI Learning Path</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Hours Empty:</strong> Study hours have not been entered<br />
<strong>A2. Invalid Format:</strong> The input contains letters or non-numeric characters, such as abc or 2h<br />
<strong>A3. Invalid Range:</strong> Study hours are ≤ 0 or &gt; 24 hours/day (suggested rule)<br />
<strong>A4. Missing Goal:</strong> No JLPT goal is set<br />
<strong>A5. Save Error:</strong> The settings could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The goal and study hours are saved.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 

### 

### 

### 5.2 Generate AI Learning Path

### 

### 

<table style="width:100%;">
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Create a personalized learning path based on skills, courses, goals, and study hours.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">An Assessment result is available; Learner has course access and has completed Hours Setup.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner confirms the creation of a Learning Path<br />
2. The system gets the Assessment result, courses, goal, and study hours<br />
3. The system sends the data to AI<br />
4. AI analyzes the data, arranges Lessons, and suggests a study schedule<br />
5. The system checks and saves the Learning Path<br />
6. The system displays the path to Learner</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Assessment:</strong> The assessment result is missing<br />
<strong>A2. Unpaid:</strong> Course access has not been activated<br />
<strong>A3. No Hours Setup:</strong> Study hours have not been set<br />
<strong>A4. Invalid AI Response:</strong> The path is missing Lessons or includes Lessons outside the course<br />
<strong>A5. AI Unavailable:</strong> The path cannot be created<br />
<strong>A6. Save Failed:</strong> The result could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">A valid Learning Path is saved and displayed.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 

### 

### 

### 5.3 Track Learning Path Progress

### 

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Track the progress of Lessons, learning activities, and milestones.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in and has a Learning Path.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner selects Learning Process<br />
2. The system gets the Learning Path and Lesson statuses<br />
3. The system gets Lesson and Milestone Exam data<br />
4. The system calculates the completion percentage<br />
5. The system displays completed and incomplete content<br />
6. Learner selects a Lesson to continue studying</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Path:</strong> No Learning Path is available<br />
<strong>A2. No Progress:</strong> Learning has not started; display 0%<br />
<strong>A3. Completed:</strong> Display the completed status<br />
<strong>A4. Retrieval Failed:</strong> Progress could not be loaded</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">Progress is displayed without automatically changing any data.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 5.4 Recalibrate Learning Path Schedule

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Adjust the learning path after failing a Milestone Exam so the Learner can review weak areas.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">A Learning Path is available and the Learner has failed a Milestone Exam.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. The system detects a failed Milestone Exam<br />
2. The system gets the score, analysis, and progress<br />
3. The system sends the data to AI<br />
4. AI identifies Lessons to review and suggests a schedule<br />
5. The system checks and saves the new path<br />
6. Learner views the path and continues reviewing</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Passed Exam:</strong> Do not start review; continue or finish the course<br />
<strong>A2. No Result:</strong> No valid result is available<br />
<strong>A3. AI Failed:</strong> Keep the current path<br />
<strong>A4. Invalid Path:</strong> Lessons are outside the course or the schedule is invalid<br />
<strong>A5. Save Failed:</strong> Keep the saved version</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Learning Path is updated if the changes are valid.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 

## 

## 

## 6. Course Learning

### 6.1 Study Lesson

### 

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Study Lessons and use Study Materials, Flashcards, and Exercise Quizzes.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in, has course access, and the Lesson is available.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner opens My Courses and selects a course<br />
2. The system displays the Lesson list<br />
3. Learner opens Lesson Detail<br />
4. Learner uses Study Materials, Flashcards, and Exercise Quizzes<br />
5. The system records learning activities<br />
6. The system updates progress<br />
7. Learner continues studying or moves to a Milestone Exam</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Access:</strong> Learner does not have course access<br />
<strong>A2. Lesson Unavailable:</strong> The Lesson is not available<br />
<strong>A3. Materials Failed:</strong> Study Materials could not be loaded<br />
<strong>A4. No Flashcard:</strong> The Lesson has no Flashcards<br />
<strong>A5. No Exercise:</strong> The Lesson has no quiz<br />
<strong>A6. Submission Failed:</strong> The Exercise could not be submitted<br />
<strong>A7. Progress Failed:</strong> Progress could not be updated</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">Valid learning activities and progress are recorded.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 

## 

## 

## 

## 

## 7. Milestone Examination

### 7.1 Take Milestone Exam

### 

<table style="width:100%;">
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Take a test at a learning milestone and determine Pass/Fail.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in, meets the exam requirements, and the exam has been set up.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner selects Milestone Exam<br />
2. The system displays exam information and opens Exam Room<br />
3. Learner answers the questions and submits the exam<br />
4. The system saves and scores it using the answers and scoring rules<br />
5. The system compares the score with the passing score to determine Pass/Fail<br />
6. The system sends the result to AI for analysis and saves it<br />
7. If Pass, continue or finish; if Fail, adjust the Learning Path</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Exam Unavailable:</strong> The exam is not open yet<br />
<strong>A2. Not Eligible:</strong> Learner does not meet the requirements<br />
<strong>A3. Required Answers Missing:</strong> Required questions are unanswered<br />
<strong>A4. Time Expired:</strong> Time has run out; follow the exam settings<br />
<strong>A5. Submission Failed:</strong> The submission could not be saved<br />
<strong>A6. Scoring Failed:</strong> Pass/Fail is not available yet<br />
<strong>A7. AI Failed:</strong> Still save the scored result</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The result and Pass/Fail status are saved.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 

## 

## 

## 8. JLPT Mock Examination

### 8.1 Take JLPT Mock Exam

### 

<table style="width:100%;">
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Take a JLPT Mock Exam at the selected level.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Learner is logged in; a matching exam or question bank is available.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner opens JLPT Mock Exam<br />
2. Learner selects a level from N5–N1<br />
3. The system displays available exams<br />
4. Learner selects an exam and starts<br />
5. The system loads questions or asks AI to put questions together from the Question Bank<br />
6. Learner takes the exam in Exam Room and submits it<br />
7. The system saves and scores the exam and displays the result</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Level:</strong> No JLPT level is selected<br />
<strong>A2. No Exam:</strong> No matching exam is available<br />
<strong>A3. AI Generation Failed:</strong> No valid set of questions is created<br />
<strong>A4. Missing Answers:</strong> Required questions are unanswered<br />
<strong>A5. Time Expired:</strong> Follow the configured time rules<br />
<strong>A6. Submission Failed:</strong> The exam could not be submitted<br />
<strong>A7. Scoring Failed:</strong> The result is not ready</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">Answers and mock exam scores are saved.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 

## 9. Exam Result Management

### 9.1 View Exam Result & Solutions

### 

<table style="width:100%;">
<colgroup>
<col style="width: 21%" />
<col style="width: 28%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Learner</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">View results, answers, solutions, and analysis after a Mock Exam or Milestone Exam.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">The exam has been submitted and the result has been saved.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Learner selects View Exam Result &amp; Solutions<br />
2. The system gets and displays the score and status<br />
3. Learner selects to review the answers<br />
4. The system displays available answers and solutions<br />
5. The system displays AI analysis<br />
6. For a Mock Exam, Learner can view recommended courses; for a Milestone Exam, continue studying or review</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Exam Not Submitted:</strong> No result is available<br />
<strong>A2. Result Pending:</strong> Scoring is not finished<br />
<strong>A3. No Solutions:</strong> Solutions could not be loaded<br />
<strong>A4. AI Pending:</strong> Analysis is not available yet<br />
<strong>A5. Unauthorized Result:</strong> The result does not belong to this account<br />
<strong>A6. Retrieval Failed:</strong> The result could not be loaded</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">Learner views the result to continue with the appropriate next step.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 10. Course Management

### 10.1 Manage Course

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Manage course information and access content management features.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in and has permission to manage Courses.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager opens Course Management<br />
2. The system lists courses<br />
3. Manager selects a course and views its information<br />
4. Manager selects the edit function<br />
5. Manager updates the content<br />
6. The system checks, saves, and shows the result</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Course:</strong> The list is empty<br />
<strong>A2. Course Not Found:</strong> The course does not exist<br />
<strong>A3. Empty Title:</strong> The course title is empty<br />
<strong>A4. Invalid JLPT Level:</strong> The level is not N5–N1<br />
<strong>A5. Invalid Price:</strong> The price is not a number or is &lt; 0<br />
<strong>A6. Invalid Duration:</strong> Duration is ≤ 0<br />
<strong>A7. Unauthorized:</strong> Manager does not have permission<br />
<strong>A8. Save Failed:</strong> Data could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">Valid course information is updated.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 10.2 Manage Learning Lesson

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Edit Lessons and study materials within a course.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in, the course exists, and Manager has permission to manage it.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager opens a course in Course Management<br />
2. The system displays Lessons<br />
3. Manager selects a Lesson<br />
4. Manager edits Lesson Title, Description, and content<br />
5. Manager adds or edits Study Materials<br />
6. Manager confirms<br />
7. The system checks and saves the content</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Lesson Not Found:</strong> The Lesson does not exist<br />
<strong>A2. Empty Title:</strong> The Lesson title is empty<br />
<strong>A3. Invalid Order:</strong> The order is not a positive whole number<br />
<strong>A4. Invalid Material:</strong> The title, content, or link is missing<br />
<strong>A5. Unsupported Format:</strong> The file type is not supported<br />
<strong>A6. Upload Failed:</strong> The file could not be uploaded<br />
<strong>A7. Save Failed:</strong> The Lesson could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Lesson and Study Materials are updated.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 10.3 Organize Learning Lesson

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Arrange and organize the order of Lessons within a Course.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Course and Lessons are available; Manager has permission to manage them.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager selects Organize Learning Lesson<br />
2. The system displays the Lesson structure<br />
3. Manager arranges the Lesson order<br />
4. Manager checks the content links<br />
5. Manager confirms<br />
6. The system checks and saves the new order</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Lessons:</strong> There are no Lessons yet<br />
<strong>A2. Duplicate Order:</strong> Two Lessons have the same position when positions must be unique<br />
<strong>A3. Invalid Order:</strong> The value is not a positive whole number<br />
<strong>A4. Missing Lesson:</strong> The referenced Lesson does not exist<br />
<strong>A5. Cancelled:</strong> Changes are not saved<br />
<strong>A6. Save Failed:</strong> Keep the previous order</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Lesson structure is saved correctly.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 

### 

### 10.4 Update Course

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Update a course after editing Lessons, Exercises, and Milestone Exams.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in and has permission; the course exists.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager reviews the edited content<br />
2. The system displays Course Information<br />
3. Manager checks Lessons, Materials, Exercises, and Milestones<br />
4. Manager edits information if needed<br />
5. Manager selects Update Course<br />
6. The system checks, saves, and shows a success message</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Required Information:</strong> A title or required field is missing<br />
<strong>A2. Invalid Price:</strong> The price is &lt; 0 or has an incorrect format<br />
<strong>A3. Invalid Duration:</strong> The duration is invalid<br />
<strong>A4. Invalid Lesson Structure:</strong> A link or order is incorrect<br />
<strong>A5. Invalid Exam Setup:</strong> A Milestone Exam does not meet the requirements to be available<br />
<strong>A6. Cancelled:</strong> Keep the old data<br />
<strong>A7. Update Failed:</strong> Success cannot be confirmed</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Course is updated with the saved content.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 11. Exercise Management

### 11.1 Manage Exercise

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Create and edit Exercises belonging to Lessons.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in; the Lesson exists; Manager has permission.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager opens a Lesson and Manage Exercise<br />
2. The system displays Exercises<br />
3. Manager selects create or edit<br />
4. Manager enters the Exercise title and settings<br />
5. Manager selects questions or Generate Exercise<br />
6. Manager checks and saves<br />
7. The system checks and links the Exercise to the Lesson</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Empty Title:</strong> The Exercise title is missing<br />
<strong>A2. No Questions:</strong> There are no questions yet<br />
<strong>A3. Invalid Reference:</strong> A question does not exist<br />
<strong>A4. No Correct Answer:</strong> The correct answer is missing<br />
<strong>A5. Invalid Passing Score:</strong> The passing score is outside the score range<br />
<strong>A6. AI Failed:</strong> Switch to adding questions manually<br />
<strong>A7. Save Failed:</strong> The Exercise could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Exercise is saved and linked to the Lesson.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 

### 

### 11.2 Generate Exercise

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Put questions together to create a practice Exercise based on a Lesson or the Question Bank.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in; the Lesson and source data are available.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager selects Generate Exercise<br />
2. The system gets the Lesson or Question Bank<br />
3. The system sends a request to AI<br />
4. AI puts together questions and answers<br />
5. The system displays the content<br />
6. Manager reviews, edits, and confirms<br />
7. The system saves the approved Exercise</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Source:</strong> No source data is available<br />
<strong>A2. AI Unavailable:</strong> AI cannot process the request<br />
<strong>A3. Empty Response:</strong> No questions are created<br />
<strong>A4. Invalid Question:</strong> A valid question or answer is missing<br />
<strong>A5. Rejected:</strong> Manager asks to generate again or edit<br />
<strong>A6. Save Failed:</strong> The content could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The AI-generated Exercise is saved after Manager confirms it.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 12. Question Bank Management

### 12.1 Manage Question Bank

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Add, edit, and import questions by JLPT level.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in and has permission to manage the Question Bank.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager opens Question Bank Management<br />
2. The system displays questions<br />
3. Manager selects add, edit, or import<br />
4. Manager enters content, question type, JLPT Level, and answers<br />
5. Manager checks and saves<br />
6. The system checks and updates the Question Bank</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Empty Question:</strong> Question content is empty<br />
<strong>A2. Invalid Type:</strong> The question type is not supported<br />
<strong>A3. Invalid Level:</strong> The level is not N5–N1<br />
<strong>A4. Missing Options:</strong> Multiple-choice options are missing<br />
<strong>A5. Missing Answer:</strong> The correct answer is missing<br />
<strong>A6. Invalid Correct Answer:</strong> The correct answer does not match the options<br />
<strong>A7. Invalid Import:</strong> The file has the wrong structure<br />
<strong>A8. Unsupported Format:</strong> The file type is not supported<br />
<strong>A9. Save Failed:</strong> Data could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Question Bank contains valid questions and answers.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

## 

## 

## 

## 13. Exam Management

### 13.1 Create Milestone Exam

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Create a Milestone Exam linked to a learning milestone in a Course.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in; the Course exists; Manager has Exam Management permission.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager opens Exam Management<br />
2. Manager selects a Course and Create Exam<br />
3. The system displays the form<br />
4. Manager selects Milestone Exam and enters information<br />
5. Manager selects AI or Question Bank to create questions<br />
6. Manager reviews the exam, arranges the questions, and links the learning milestone<br />
7. Manager sets up and saves the exam</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Empty Title:</strong> The exam title is missing<br />
<strong>A2. No Course:</strong> No course has been selected<br />
<strong>A3. Invalid Type:</strong> The Exam type is incorrect<br />
<strong>A4. No Questions:</strong> There is no set of questions<br />
<strong>A5. Invalid Reference:</strong> A question does not exist<br />
<strong>A6. AI Failed:</strong> Choose to enter questions manually<br />
<strong>A7. Missing Milestone:</strong> No learning milestone is linked<br />
<strong>A8. Save Failed:</strong> The exam could not be created</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The Milestone Exam is saved and linked to the Course and learning milestone.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 13.2 Generate Exam

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 29%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>AI System</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">AI puts together questions from the Question Bank to create an exam.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in, has selected an exam, and has question data.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager selects Generate Exam<br />
2. The system gets questions from the Question Bank<br />
3. The system sends a request to AI<br />
4. AI puts together the questions<br />
5. The system displays the generated exam<br />
6. Manager reviews, adjusts, and confirms<br />
7. The system saves the questions in the exam</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. No Data:</strong> The Question Bank is empty<br />
<strong>A2. Insufficient Questions:</strong> There are not enough questions<br />
<strong>A3. AI Error:</strong> AI cannot process the request<br />
<strong>A4. Empty Exam:</strong> AI returns an empty result<br />
<strong>A5. Invalid Question:</strong> Question content or an answer is missing<br />
<strong>A6. Duplicate Question:</strong> Questions are repeated outside the allowed rules<br />
<strong>A7. Manager Rejects:</strong> Manager requests a new version<br />
<strong>A8. Save Failed:</strong> The exam could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">The question set is checked and linked to the Exam.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

###  

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 13.3 Configure Exam

### 

<table>
<colgroup>
<col style="width: 21%" />
<col style="width: 29%" />
<col style="width: 20%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th>Primary Actors</th>
<th>Manager</th>
<th>Secondary Actors</th>
<th>None</th>
</tr>
<tr class="odd">
<th>Description</th>
<th colspan="3">Set the time limit, scoring scale, passing score, question order, and Exam status.</th>
</tr>
<tr class="header">
<th>Preconditions</th>
<th colspan="3">Manager is logged in; the Exam exists and has a valid question set.</th>
</tr>
<tr class="odd">
<th>Normal Flow</th>
<th colspan="3">1. Manager selects an Exam and Configure Exam<br />
2. The system displays the exam information<br />
3. Manager sets the duration and scoring scale<br />
4. Manager sets the passing score, question order, and status<br />
5. Manager selects Save Configuration<br />
6. The system checks and saves the settings</th>
</tr>
<tr class="header">
<th>Alternative Flows</th>
<th colspan="3"><strong>A1. Missing Fields:</strong> Required information is missing<br />
<strong>A2. Invalid Duration:</strong> The time limit is not a positive whole number<br />
<strong>A3. Invalid Passing Score:</strong> The passing score is &lt; 0 or &gt; the maximum score<br />
<strong>A4. Invalid Maximum Score:</strong> The maximum score is ≤ 0<br />
<strong>A5. Invalid Order:</strong> The question order is invalid<br />
<strong>A6. Empty Questions:</strong> There are not enough questions<br />
<strong>A7. Invalid Status:</strong> The status is not supported<br />
<strong>A8. Save Failed:</strong> The settings could not be saved</th>
</tr>
<tr class="odd">
<th>Postconditions</th>
<th colspan="3">Valid Exam settings are saved.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

### 

# III. Functional Requirements

## 1. Public & Guest Features 

### 1.1 Home Page & Navigation 

#### 1.1.1 View Home Page Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image17.png" style="width:6.28438in;height:3.94444in" />

This screen allows the Guest to:

- **View Platform Introduction:** view general information about the Japanese Exam Preparation System (JEPS).

- **Browse Featured Courses:** view highlighted courses along with their JLPT level and pricing.

- **Navigate Public Sections:** access public pages such as About Us, Course List, and JLPT Mock Exam information.

On the screen, s/he can also:

- Choose to go to the **User Login** or **Register Account** screens by clicking the respective header buttons.

- Start using the system by clicking the **Get Started Now** call-to-action button.

**Field Description**

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 88%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Field Name</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>(1)</th>
<th><p>Initial values: "Home", "About Us", "Course List", "JLPT Mock Exam"</p>
<p>Hover the mouse to show the field name: "Header Navigation Menu"</p></th>
</tr>
<tr class="header">
<th>(2)</th>
<th><p>Initial value: None (Empty)</p>
<p>Action buttons to redirect Guest to Register Account or User Login screen.</p></th>
</tr>
<tr class="odd">
<th>(3)</th>
<th>Read-only container displaying banner headline and "Get Started Now" CTA button.</th>
</tr>
<tr class="header">
<th>(4)</th>
<th><p>Initial value: Displayed featured course list</p>
<p>Clicking "View Details" redirects user to the corresponding Course Details screen.</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#### 1.1.2 View About Us Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image45.png" style="width:6.28438in;height:3.58333in" />

This screen allows the Guest to:

- **Read System Mission:** view background information, platform mission, and core feature introductions of JEPS.

- **Explore Learning Approach:** understand the AI-driven personalized learning path methodology.

On the screen, s/he can also:

- Navigate back to other public sections using the top navigation header.

**Field Description**

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 88%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Field Name</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>(1)</th>
<th><p>Initial values: Header navigation links</p>
<p>Hover the mouse to show the field name: "Navigation Header</p></th>
</tr>
<tr class="header">
<th>(2)</th>
<th>Read-only text box detailing platform features, AI path generation, and mission statements.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

### 1.2 Account Registration & Recovery 

#### 1.2.1 Register Account Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image4.png" style="width:6.28438in;height:3.59722in" />

This screen allows the Guest to:

- **Provide Registration Details:** enter full name, email, password, and password confirmation.

- **Create New Learner Account:** register a new account in the system upon validating inputs.

On the screen, s/he can also:

- Choose to go to the **User Login** screen by clicking the Login link if an account already exists.

**Field Description**

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 88%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Field Name</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>(1)</th>
<th><p>Input text. Required. Max length 100 characters.</p>
<p>Hover the mouse to show the field name: "Full Name"</p></th>
</tr>
<tr class="header">
<th>(2)</th>
<th>Input text. Required. Must follow valid email format and be unique in the system.</th>
</tr>
<tr class="odd">
<th>(3)</th>
<th><p>Input password. Required. Min 8, max 64 characters.</p>
<p>Hover the mouse to show the field name: "Password / Confirm Password"</p></th>
</tr>
<tr class="header">
<th>(4)</th>
<th>Action button. Validates form inputs and creates a new Learner account.</th>
</tr>
<tr class="odd">
<th>(5)</th>
<th>Navigation link to redirect user to the User Login screen.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#### 1.2.2 Reset Password Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image34.png" style="width:6.28438in;height:3.70833in" />

This screen allows the Guest/User to:

- **Submit Password Recovery Request:** enter registered email to request a password reset link.

On the screen, s/he can also:

- Return to the **User Login** screen by clicking the Back to Login link.

**Field Description**

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 88%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Field Name</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>(1)</th>
<th><p>Input text. Required. Registered email address in the system.</p>
<p>Hover the mouse to show the field name: "Registered Email"</p></th>
</tr>
<tr class="header">
<th>(2)</th>
<th>Action button. Triggers sending the password reset link to user's email.</th>
</tr>
<tr class="odd">
<th>(3)</th>
<th><p>Input password. Required. Min 8, max 64 characters.</p>
<p>Hover the mouse to show the field name: "Password / Confirm Password"</p></th>
</tr>
<tr class="header">
<th>(4)</th>
<th>Navigation link to return to the User Login screen.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

### 1.3 Public Course & Exam Browsing

### 

### 

#### 1.3.1 View Course List Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image13.png" style="width:6.28438in;height:3.43056in" />

This screen allows the Guest to:

- **View Course Catalog:** view all available public courses in the system.

- **Filter Courses:** filter courses by JLPT levels (N5 to N1).

- **Search Courses:** enter keyword(s) to search courses by title or description.

On the screen, s/he can also:

- Click on any course card to navigate to the **Course Details** screen.

**Field Description**

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 88%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Field Name</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>(1)</th>
<th><p>Input text and dropdown filter.</p>
<p>Initial value: All JLPT levels.</p>
<p>Hover the mouse to show the field name: "Course Search &amp; Filter"</p></th>
</tr>
<tr class="header">
<th>(2)</th>
<th>Display grid of course cards containing title, JLPT badge, and fee.</th>
</tr>
<tr class="odd">
<th>(3)</th>
<th>Action link/button to view full details of a specific course.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#### 1.3.2 View Course Details Screen (Course Information)

<img src="Report-3_Software_Requirement_Specification_media/media/image15.png" style="width:6.28438in;height:3.84722in" />

This screen allows the Guest/Learner to:

- **View Course Header:** view the course title, target JLPT level, and enrollment price.

- **Review Course Details:** read comprehensive course descriptions, learning objectives, and curriculum breakdown.

- **Inspect Syllabus Structure:** explore specific study modules (e.g., Vocabulary & Kanji, Grammar, Reading & Listening).

On the screen, s/he can also:

- Click the **Enroll / Register Now** button to proceed with purchasing or enrolling in the course.

- Return to other public sections or authenticate using the header topbar.

**Field Description**

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 88%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Field Name</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>(1)</th>
<th><p>Read-only text displaying course name and target level badge.</p>
<p>Hover the mouse to show the field name: "Course Title &amp; Level"</p></th>
</tr>
<tr class="header">
<th>(2)</th>
<th><p>Read-only numeric display showing course pricing in VNĐ.</p>
<p>Hover the mouse to show the field name: "Course Price Tag"</p></th>
</tr>
<tr class="odd">
<th>(3)</th>
<th><p>Action button to initiate the enrollment and checkout process.</p>
<p>Redirects to Checkout Screen (if logged in) or User Login Screen (if guest).</p></th>
</tr>
<tr class="header">
<th>(4)</th>
<th>Read-only text containers and interactive module cards detailing course overview and syllabus breakdown.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#### 1.3.3 View JLPT Mock Exam Screen (JLPT Mock Exam Information)

<img src="Report-3_Software_Requirement_Specification_media/media/image22.png" style="width:6.28438in;height:3.80556in" />

This screen allows the Guest to:

- **Browse JLPT Mock Exams:** view available public practice tests categorized by level.

- **Filter Exams by Level:** filter mock tests by specific JLPT difficulty levels (N5 to N1) or view all levels.

- **View Test Information:** review key test parameters such as target level, total questions (e.g., 60 questions), duration (e.g., 105 minutes), and included test sections.

On the screen, s/he can also:

- Click **Take Test** to begin the mock examination (prompts for login if accessing restricted test features).

- Click **View Details** to inspect full information regarding the test structure.

**Field Description**

<table>
<colgroup>
<col style="width: 11%" />
<col style="width: 88%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Field Name</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>(1)</th>
<th><p>Tab/Button filter group (All Levels, N5, N4, N3, N2, N1).</p>
<p>Hover the mouse to show the field name: "Exam Level Filter"</p></th>
</tr>
<tr class="header">
<th>(2)</th>
<th>Read-only exam cards displaying test title, target level, total question count, duration, and test sections.</th>
</tr>
<tr class="odd">
<th>(3)</th>
<th>Action buttons ("Take Test" and "View Details") for initiating the exam or viewing detailed exam specifications.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 2. Learner Features

### 2.1 Authentication & Profile Management 

#### 2.1.1 User Login Screen 

<img src="Report-3_Software_Requirement_Specification_media/media/image39.png" style="width:6.28438in;height:3.93056in" />

This screen allows the user to:

- Log in to JEPS using a registered account.

- Log in using Google authentication.

- Navigate to the registration screen.

- Request a password reset when the password is forgotten.

**Field Description**

| **Field Name** | **Description**                                                                                                                                   |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the user to enter a registered username or email address. Required field.                                                                  |
| \(2\)          | Allows the user to enter the account password. Input characters are masked. Required field.                                                       |
| \(3\)          | Validates the credentials and authenticates the user. Upon successful login, redirects the user to the appropriate Dashboard based on their role. |
| \(4\)          | Initiates Google authentication.                                                                                                                  |
| \(5\)          | Navigates to the User Register screen.                                                                                                            |
| \(6\)          | Navigates to the Password Reset screen.                                                                                                           |

#### 2.1.2 Learner Dashboard Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image10.png" style="width:6.28438in;height:3.97222in" />

This screen allows the Learner to:

- View their current JLPT target level and learning progress.

- View their daily study goal and completed study hours.

- View upcoming examinations.

- Continue learning from the most recent lesson.

- Access the placement assessment.

- Recalibrate the learning path schedule.

- Navigate to courses, learning progress, examination history, order history, and profile information.

**Field Description**

| **Field Name** | **Description**                                                                                              |
|----------------|--------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the learner to search for available learning content.                                                 |
| \(2\)          | Provides access to Dashboard, My Courses, Learning Process, JLPT Mock Exam, Exam History, and Order History. |
| \(3\)          | Displays the learner's name and avatar.                                                                      |
| \(4\)          | Displays the target JLPT level and a progress bar indicating learning progress.                              |
| \(5\)          | Displays completed study hours compared with the daily target.                                               |
| \(6\)          | Displays the next scheduled examination and remaining days.                                                  |
| \(7\)          | Displays the most recent lesson and current learning activity.                                               |
| \(8\)          | Navigates to the learner's most recent lesson.                                                               |
| \(9\)          | Navigates to the Place Assessment Exam screen.                                                               |
| \(10\)         | Opens the learning path schedule recalibration function.                                                     |

#### 2.1.3 View Profile Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image28.png" style="width:6.28438in;height:3.94444in" />

This screen allows the Learner to:

- View personal information.

- View current and target JLPT levels.

- Navigate to the Edit Profile screen.

- Navigate to the Change Password screen.

**Field Description**

| **Field Name** | **Description**                                      |
|----------------|------------------------------------------------------|
| \(1\)          | Provides navigation to the learner's main functions. |
| \(2\)          | Displays the learner's registered full name.         |
| \(3\)          | Displays the learner's registered email address.     |
| \(4\)          | Displays the learner's phone number.                 |
| \(5\)          | Displays the learner's recorded current JLPT level.  |
| \(6\)          | Displays the learner's target JLPT level.            |
| \(7\)          | Navigates to the Change Password screen.             |
| \(8\)          | Navigates to the Edit Profile screen.                |
| \(9\)          | Logs the learner out of JEPS.                        |

#### 2.1.4 Edit Profile Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image21.png" style="width:6.28438in;height:4in" />

This screen allows the Learner to:

- View and update personal information.

- Edit their full name, email address, and phone number.

- Save profile changes.

- Cancel editing and return to the User Profile screen.

**Field Description**

| **Field Name** | **Description**                                                                                      |
|----------------|------------------------------------------------------------------------------------------------------|
| \(1\)          | Displays the current full name and allows the learner to edit it. Required field.                    |
| \(2\)          | Displays the registered email and allows the learner to update it. Must follow a valid email format. |
| \(3\)          | Allows the learner to update their phone number.                                                     |
| \(4\)          | Discards unsaved changes and returns to User Profile.                                                |
| \(5\)          | Validates and saves the updated profile information.                                                 |
| \(6\)          | Provides access to other learner functions.                                                          |
| \(7\)          | Logs the learner out of JEPS.                                                                        |

#### 2.1.5 Change Password Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image3.png" style="width:6.28438in;height:3.95833in" />

This screen allows the Learner to:

- Change the current account password.

- Enter and confirm a new password.

- Cancel the password change.

- Submit a password change request.

**Field Description**

| **Field Name** | **Description**                                                                           |
|----------------|-------------------------------------------------------------------------------------------|
| \(1\)          | Allows the learner to enter their current password for verification. Required field.      |
| \(2\)          | Allows the learner to enter a new password that meets the system's password requirements. |
| \(3\)          | Requires the learner to re-enter the new password. Must match the New Password field.     |
| \(4\)          | Cancels the password change and returns to User Profile.                                  |
| \(5\)          | Validates the input and updates the password if all requirements are satisfied.           |
| \(6\)          | Provides access to learner functions.                                                     |
| \(7\)          | Logs the learner out of JEPS.                                                             |

### 2.2 Assessment & AI Personalized Learning Path 

#### 2.2.1 Take Place Assessment Exam Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image37.png" style="width:6.28438in;height:3.88889in" />

This screen allows the Learner to:

- View the selected JLPT assessment level.

- Complete assessment questions in different skill sections.

- Monitor the remaining assessment time and progress.

- Navigate between questions.

- Submit answers to complete the assessment.

**Field Description**

| **Field Name** | **Description**                                                                   |
|----------------|-----------------------------------------------------------------------------------|
| \(1\)          | Displays the selected JLPT level for the assessment, such as N3.                  |
| \(2\)          | Displays the remaining time in minutes and seconds.                               |
| \(3\)          | Displays Vocabulary, Reading, Listening, and Grammar sections.                    |
| \(4\)          | Displays the current question number and total number of questions, such as 5/20. |
| \(5\)          | Visually indicates progress through the assessment.                               |
| \(6\)          | Displays the current Japanese assessment question.                                |
| \(7\)          | Displays the possible answers and allows the learner to select one option.        |
| \(8\)          | Returns to the previous question.                                                 |
| \(9\)          | Moves to the next question.                                                       |
| \(10\)         | Submits the assessment and ends the attempt after confirmation.                   |

#### 2.2.2 JLPT Target and Study Check Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image11.png" style="width:6.28438in;height:3.97222in" />

This pop-up allows the Learner to:

- Select the target JLPT level.

- Confirm whether they have previously studied any material at their current level.

- Provide learning background information for personalization.

- Continue to the Hours Setup pop-up.

**Field Description**

| Field Name | Description                                                                                              |
|------------|----------------------------------------------------------------------------------------------------------|
| \(1\)      | Allows the learner to select a target level from N5, N4, N3, N2, or N1. Only one option can be selected. |
| \(2\)      | Asks whether the learner has studied any material at their current JLPT level before.                    |
| \(3\)      | Indicates that the learner has previously studied some material at the current level.                    |
| \(4\)      | Indicates that the learner has not studied any material at the current level.                            |
| \(5\)      | Validates the selected information and opens the Hours Setup pop-up.                                     |
| \(6\)      | Closes the pop-up and returns to the underlying screen.                                                  |

#### 2.2.3 Hours Setup Screen<img src="Report-3_Software_Requirement_Specification_media/media/image9.png" style="width:6.28438in;height:3.97222in" />

This pop-up allows the Learner to:

- Set the number of study hours per day.

- Specify the number of study days per week.

- Configure a weekly study schedule.

- Continue to AI Learning Path Generation.

**Field Description**

| **Field Name** | **Description**                                                                          |
|----------------|------------------------------------------------------------------------------------------|
| \(1\)          | Allows the learner to specify the number of hours available for studying each day.       |
| \(2\)          | Allows the learner to specify the number of study days per week, from 1 to 7.            |
| \(3\)          | Validates and saves the study schedule before navigating to AI Learning Path Generation. |
| \(4\)          | Closes the pop-up without saving unsaved changes.                                        |

#### 2.2.4 Generate AI Learning Path Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image5.png" style="width:6.28438in;height:3.93056in" />

This screen allows the Learner to:

- Review their placement assessment summary.

- View their assessed JLPT level and target JLPT level.

- Review the configured weekly study goal.

- View their strongest skills and skills requiring improvement.

- Request the generation of a personalized learning path using AI.

**Field Description**

| **Field Name** | **Description**                                                                                                                     |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Displays the learner's assessed JLPT level based on the placement assessment result.                                                |
| \(2\)          | Displays the target JLPT level selected by the learner.                                                                             |
| \(3\)          | Displays the weekly study hours calculated from the Hours Setup information.                                                        |
| \(4\)          | Displays the learner's strongest skill identified from the assessment.                                                              |
| \(5\)          | Displays the skills that require additional practice and improvement.                                                               |
| \(6\)          | Initiates AI generation of a personalized learning path based on the learner's assessment result, target level, and study schedule. |
| \(7\)          | Returns to the previous step to review or adjust the provided information.                                                          |

## 

### 2.5 Commerce & Orders 

#### 2.5.1 View Recommended Course 

<img src="Report-3_Software_Requirement_Specification_media/media/image30.png" style="width:6.28438in;height:3.59722in" />

This screen allows the Learner to:

- View recommended courses based on their placement assessment results and current JLPT level.

- View the recommended course's name, number of lessons, covered skills, difficulty level, and price.

- Browse other available JLPT courses across different levels (N5, N3, N2, and N1).

- Compare course information and prices before making a purchase decision.

- View detailed information about a selected course by clicking the View Details button.

- Navigate through the list of other JLPT courses using the previous and next buttons.

**Field Description**

| **Field Name** | **Description**                                                                                                      |
|----------------|----------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Learner to view detailed information about the selected course.                                           |
| \(2\)          | Allows the Learner to navigate through the list of other available JLPT courses using the previous and next buttons. |

#### 2.5.2 Register for Course Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image14.png" style="width:6.28438in;height:3.61111in" />

This screen allows the Learner to:

- Return to the recommended courses page.

- View the selected course's details, including its name, number of lessons, covered skills, JLPT level, and price.

- Register for the selected course.

- View the course overview, including its description and expected learning outcomes.

- View the course content, including the lessons and learning materials included in the course.

**Field Description**

| **Field Name** | **Description**                                                                                             |
|----------------|-------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Learner to return to the recommended courses page.                                               |
| \(2\)          | Allows the Learner to register for the selected course and proceed with the enrollment or purchase process. |
| \(3\)          | Displays the course description, learning objectives, and expected learning outcomes.                       |
| \(4\)          | Displays the course structure, including its lessons and learning materials.                                |

#### 2.5.3 Checkout Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image33.png" style="width:6.28438in;height:3.61111in" />

This screen allows the Learner to:

- View the checkout process, including Cart, Order Information, Payment, and Complete.

- Review the selected course's information, including its name, number of lessons, JLPT level, and total price.

- View and enter billing information, including full name, email address, and phone number.

- Proceed to the payment step after reviewing the order summary and billing information.

**Field Description**

| **Field Name** | **Description**                                                                                                                          |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Displays the four steps of the checkout process: Cart, Order Information, Payment, and Complete, indicating the Learner's current stage. |
| \(2\)          | Displays and allows the Learner to enter or update billing information, including full name, email address, and phone number.            |
| \(3\)          | Allows the Learner to proceed to the payment step after reviewing the order summary and billing information.                             |

#### 2.5.4 Payment Gateway Screen (Make Payment)

<img src="Report-3_Software_Requirement_Specification_media/media/image36.png" style="width:6.28438in;height:3.59722in" />

This screen allows the Learner to:

- View the order number and course information for the current transaction.

- Review the selected payment method (VNPay QR) and the total payment amount.

- Scan the displayed QR code using a banking application to complete the payment.

**Field Description**

| **Field Name** | **Description**                                                                                       |
|----------------|-------------------------------------------------------------------------------------------------------|
| \(1\)          | Displays the QR code used by the Learner to make the payment through a supported banking application. |

#### 

#### 2.5.5 View Invoice Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image7.png" style="width:6.28438in;height:3.63889in" />

This screen allows the Learner to:

- View a confirmation message indicating that the payment has been completed successfully.

- Review invoice information, including invoice number, order number, order date, customer name, email address, and payment method.

- View the purchased course and its price.

- Review the total amount paid for the order.

- Download the invoice for record-keeping purposes.

- Navigate to the My Learning page to access the purchased course.

**Field Description**

| **Field Name** | **Description**                                                                                  |
|----------------|--------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Learner to download the invoice containing the order details and payment information. |
| \(2\)          | Allows the Learner to navigate to the My Learning page to access the purchased course.           |

#### 2.5.6 Track Order History 

<img src="Report-3_Software_Requirement_Specification_media/media/image31.png" style="width:6.28438in;height:3.59722in" />

This screen allows the Learner to:

- View their order history, including order numbers, order dates, total amounts, and order statuses.

- Filter orders by status: All Orders, Processing, Completed, and Cancelled.

- View the details of a specific order by clicking the View Detail link.

- Track the status of each order to determine whether it is being processed, completed, or cancelled.

**Field Description**

| **Field Name** | **Description**                                                                                                      |
|----------------|----------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Learner to filter the order history by status, including All Orders, Processing, Completed, and Cancelled |
| \(2\)          | Allows the Learner to view detailed information about the selected order.                                            |

#### 2.5.7 View Orders Detail Screens

<img src="Report-3_Software_Requirement_Specification_media/media/image29.png" style="width:6.28438in;height:3.625in" />

This screen allows the Learner to:

- View detailed information about a selected order, including the order number, order date, payment method, and order status.

- Check whether the order has been completed successfully.

- View the items included in the order, including item names, prices, quantities, and subtotals.

- Review the total amount of the order.

- Return to the Order History page.

**Field Description**

| **Field Name** | **Description**                                                                                                            |
|----------------|----------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Learner to return to the Order History page.                                                                    |
| \(2\)          | Displays the order number, order date, payment method, and current order status.                                           |
| \(3\)          | Displays the items included in the order, including item names, prices, quantities, subtotals, and the total order amount. |

## 

##  3. Manager Features 

### 3.1 Dashboard & Course Management 

#### 3.1.1 View Manager Dashboard Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image41.png" style="width:6.28438in;height:3.91667in" />

This screen allows the **Manager** to:

- **View System Summary**: view high-level key performance metrics including total published courses, active learners, total questions in the question bank, and active exams.

- **View Learning Activity Overview**: view graphical/visual summary of learning activities across the system.

- **View Recent Activity**: view a list of recent course, test, and vocabulary set updates or assessments.

- **Search System Data**: enter keyword(s) in the input search box to find relevant system records.

- **Navigate Modules**: access other management modules such as Course Management, Question Bank, Exam Management, and Reports & Analytics via the left navigation menu.

On the screen, s/he can also:

- **View Current User Profile**: see the currently logged-in Manager's profile information (name and role).

- **Navigate to Specific Activity Details**: click on any item under the Recent Activity list to navigate to its management or review screen.

| **Field Name** | **Description**                                                                                                                                 |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Enter search key terms to search across the system workspace                                                                                    |
| \(2\)          | Display the total number of published courses in the system                                                                                     |
| \(3\)          | Display the total number of active learners and monthly growth percentage                                                                       |
| \(4\)          | Display the total number of questions created across JLPT N5–N1 levels                                                                          |
| \(5\)          | Display the total number of currently active exams and those scheduled for the current week                                                     |
| \(6\)          | Display a chronologically ordered list of system updates, including course edits, test publications, review requests, and placement assessments |

#### 

#### 3.1.2 Manage Course Screen (bao gồm Set up Price)

<img src="Report-3_Software_Requirement_Specification_media/media/image6.png" style="width:6.28438in;height:3.93056in" />

This screen allows the **Manager** to:

- **View Course List**: view the list of all existing courses along with their key attributes such as level, total lessons, price, status, and last updated date.

- **Search Courses**: enter keyword(s) in the search box to search courses by their title.

- **Filter Course List**: filter courses by criteria such as Level and Status.

- **Sort Course List**: sort the course list using available sorting criteria (e.g., Recently updated) or by clicking column headers.

- **Paginate Course List**: navigate through multiple pages of the course list using pagination controls (Previous, page numbers 1, 2, 3, and Next).

- **Manage Individual Course**: perform quick actions on a specific course, including editing details or viewing additional options.

On the screen, s/he can also:

- **View Current User Profile**: see the currently logged-in Manager's profile information (name and role) in the top-right corner.

- **Navigate Main Modules**: switch to other management sections (Dashboard, Question Bank, Exam Management, Reports & Analytics) via the left navigation sidebar.

| **Field Name** | **Description**                                                                                        |
|----------------|--------------------------------------------------------------------------------------------------------|
| \(1\)          | Text input field to search courses by title                                                            |
| \(2\)          | Dropdown filter to filter courses by JLPT levels                                                       |
| \(3\)          | Dropdown filter to filter courses by publication status                                                |
| \(4\)          | Dropdown control to change the ordering of the list                                                    |
| \(5\)          | Column displaying the title/name of the course                                                         |
| \(6\)          | Column displaying the JLPT level targeted by the course (e.g., **N5, N4, N3, N2, N1**)                 |
| \(7\)          | Column displaying the total count of lessons included in the course                                    |
| \(8\)          | Column displaying the price of the course in USD                                                       |
| \(9\)          | Column displaying the current status of the course (e.g., **Published, Draft, Review**).               |
| \(10\)         | Column displaying the date when the course was last updated                                            |
| \(11\)         | Action links for each row                                                                              |
| \(12\)         | Controls at the bottom-right corner to switch between result pages (**\< Previous, 1, 2, 3, Next \>**) |

#### 3.1.3 Manage Learning Lesson Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image19.png" style="width:6.28438in;height:3.94444in" />

This screen allows the **Manager** to:

- **View Lesson List**: view the list of lessons belonging to a specific course (e.g., Core Grammar - JLPT N4) along with details such as order, lesson title, content type, duration, visibility status, and last edited date.

- **Add New Learning Lesson**: click the + Add Learning Lesson button to open the form for creating a new lesson for the current course.

- **Reorder Lessons**: change the learning order of lessons in the list by using the reorder action buttons (↑↓) or dragging lessons directly.

- **Edit Lesson**: click the Edit link on a specific lesson row to update its details and content.

- **Search System Data**: enter keyword(s) in the header search box to query system records.

- **Navigate Modules**: switch to other management modules (Dashboard, Course Management, Question Bank, Exam Management, Reports & Analytics) using the left navigation menu.

On the screen, s/he can also:

- **View Current Course Context**: see the target course name currently being managed (Course: Core Grammar (JLPT N4)).

- **View Current User Profile**: view the logged-in Manager's name and role in the top-right area.

- **Read Operational Guidance**: view a helper note indicating that lessons can be dragged to change order and that learners will only see published lessons.

| **Field Name** | **Description**                                                                                                                                           |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Text input field in the top header to search system data                                                                                                  |
| \(2\)          | Action button to open the lesson creation interface                                                                                                       |
| \(3\)          | Non-editable text label displaying the currently selected course title                                                                                    |
| \(4\)          | Column displaying the sequential index/order number of the lesson within the course (e.g., 01, 02, 03, 04, 05)                                            |
| \(5\)          | Column displaying the name of each lesson                                                                                                                 |
| \(6\)          | Column displaying the primary media/material format of the lesson (e.g., **Reading + examples, Text + examples, Text + notes, Text + quiz, Interactive**) |
| \(7\)          | Column displaying the estimated completion duration for each lesson in minutes (**e.g., 12 min, 18 min**)                                                 |
| \(8\)          | Column displaying the publication visibility state of the lesson (**Published or Draft**)                                                                 |
| \(9\)          | Column displaying the date when the lesson was most recently updated (e.g., **Oct 08, Oct 07**)                                                           |
| \(10\)         | Action controls for each lesson row                                                                                                                       |
| \(11\)         | Helper text placed below the table                                                                                                                        |

#### 3.1.4 Manage Exercise Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image23.png" style="width:6.28438in;height:3.93056in" />

This screen allows the **Manager** to:

- **Configure Exercise Details**: input and manage core exercise metadata, including exercise title, linked lesson, difficulty level, question count, time limit, and instructions.

- **Preview Questions**: view a real-time preview of questions, options, and correct answers associated with the exercise.

- **Add Question**: click the Add Question button to append or attach a new question to the current exercise.

- **Save Draft**: click the Save Draft button to save the current exercise configuration as a draft without publishing.

- **Publish Exercise**: click the Publish Exercise button to make the exercise active and available to learners.

- **Search System Data**: enter keyword(s) in the top input search box to query system records.

- **Navigate Modules**: switch to other management sections (Dashboard, Course Management, Question Bank, Exam Management, Reports & Analytics) via the left navigation sidebar.

On the screen, s/he can also:

- **View Current User Profile**: see the currently logged-in Manager's profile information (name and role) in the top-right corner.

- **Review Answer Keys**: view the designated correct answer for each previewed question.

| **Field Name** | **Description**                                                                                                 |
|----------------|-----------------------------------------------------------------------------------------------------------------|
| \(1\)          | Text input field in the header to search system data                                                            |
| \(2\)          | Text input field to enter or update the name of the exercise                                                    |
| \(3\)          | Dropdown/Select field to link the exercise to a specific lesson                                                 |
| \(4\)          | Dropdown/Select field to set the difficulty level of the exercise                                               |
| \(5\)          | Numeric input field to specify the total number of questions in the exercise                                    |
| \(6\)          | Numeric input field to set the time limit for completing the exercise in minutes                                |
| \(7\)          | Text area field to provide guidelines or instructions for learners taking the exercise                          |
| \(8\)          | Preview container displaying sample question text, multiple-choice options (A, B, C, D), and the correct answer |
| \(9\)          | Action button to add a new question to the exercise                                                             |
| \(10\)         | Action button to save all entered information as an unpublished draft                                           |
| \(11\)         | Action button to publish and activate the exercise for learners                                                 |

### 3.2 Question Bank & Exam Management 

#### 3.2.1 Manage Question Bank Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image16.png" style="width:6.28438in;height:3.93056in" />

This screen allows the **Manager** to:

- **View Question Bank List**: view a list of all existing questions along with key information such as ID, preview, JLPT level, question type, skill category, correct answer, and usage frequency.

- **Filter Question List**: filter questions by JLPT Level, Question Type, Skill, and Status.

- **Search Questions**: enter keyword(s) in the search box to search questions by their content or title.

- **Add Question**: click the + Add Question button to create a new single question manually.

- **Import Questions**: click the + Import Question button to upload questions in bulk via a file.

- **Edit Question**: click the Edit link on a specific row to modify an existing question.

On the screen, s/he can also:

- **View Question Bank Summary**: view the total count of active questions and the status of the last import validation.

- **View Current User Profile**: view the logged-in Manager's name and role in the top-right corner.

- **Navigate Modules**: switch to other management sections (Dashboard, Course Management, Question Bank, Exam Management, Reports & Analytics) using the left navigation menu.

| **Field Name** | **Description**                                                                                                          |
|----------------|--------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Global text input box located at the top header to search system data                                                    |
| \(2\)          | Text input field to search for specific questions in the question bank by content or keyword                             |
| \(3\)          | Dropdown option to filter questions by JLPT level                                                                        |
| \(4\)          | Dropdown option to filter questions by format (e.g., MCQ, Fill blank, Reading)                                           |
| \(5\)          | Dropdown option to filter questions by language skill (e.g., Grammar, Kanji, Vocabulary, Reading)                        |
| \(6\)          | Dropdown option to filter questions by operational status (e.g., Active, Inactive)                                       |
| \(7\)          | Button to open the interface for adding a new question manually                                                          |
| \(8\)          | Button to trigger the bulk question import process                                                                       |
| \(9\)          | Unique identification code assigned to each question in the bank                                                         |
| \(10\)         | Short text snippet or sample content representing the question                                                           |
| \(11\)         | Target JLPT level assigned to the question                                                                               |
| \(12\)         | Format or interaction model of the question                                                                              |
| \(13\)         | Specific language competence or topic assessed by the question                                                           |
| \(14\)         | Designated right choice or answer key for the question                                                                   |
| \(15\)         | Number of times the question has been used across exams and exercises                                                    |
| \(16\)         | Action links to edit the corresponding question                                                                          |
| \(17\)         | Informational text line at the bottom displaying the total active questions count and last import file validation status |

#### 3.2.2 Manage Exam Screen (Milestone, JLPT Mock, Place Assessment)

<img src="Report-3_Software_Requirement_Specification_media/media/image8.png" style="width:6.28438in;height:3.94444in" />

This screen allows the **Manager** to:

- **View Exam List**: view a list of all existing exams along with key parameters such as title, type, JLPT level, duration, question count, status, and available actions.

- **Filter Exam List**: filter exams by Exam Type, JLPT Level, and Status.

- **Search Exams**: enter keyword(s) in the search box to search exams by title.

- **Create Exam**: click the + Create Exam button to manually construct a new exam.

- **Generate Exam**: click the + Generate Exam button to automatically generate an exam from the question bank based on predefined rules.

- **Edit Exam**: click the Edit link on a specific row to modify exam configuration.

- **View Exam Results**: click the Results link on a specific row to view performance metrics and student submission details.

On the screen, s/he can also:

- **View Exam Category Summary**: view total counts and categories of exams, including Milestone Tests, JLPT Mock Exams, and Placement Assessments.

- **View Current User Profile**: view the logged-in Manager's name and role in the top-right area.

- **Navigate Modules**: switch to other management sections (Dashboard, Course Management, Question Bank, Exam Management, Reports & Analytics) using the left navigation menu.

| **Field Name** | **Description**                                                                       |
|----------------|---------------------------------------------------------------------------------------|
| \(1\)          | Global text input box located in the top header to search system data                 |
| \(2\)          | Text input field to search for specific exams by title or keyword                     |
| \(3\)          | Dropdown option to filter exams by category (e.g., Milestone, Mock Exam, Placement)   |
| \(4\)          | Dropdown option to filter exams by JLPT level (e.g., N5–N1)                           |
| \(5\)          | Dropdown option to filter exams by status (e.g., Scheduled, Published, Active, Draft) |
| \(6\)          | Button to open the manual exam creation form                                          |
| \(7\)          | Button to launch the automated exam generation wizard                                 |
| \(8\)          | Display card showing total count and label for course progress checkpoint exams       |
| \(9\)          | Display card showing total count and label for full-length JLPT practice exams        |
| \(10\)         | Display card showing total count and label for level diagnosis exams                  |
| \(11\)         | Column displaying the title/name of the exam                                          |
| \(12\)         | Column displaying the classification type of the exam                                 |
| \(13\)         | Column displaying the target JLPT level for the exam                                  |
| \(14\)         | Column displaying the allocated time limit for the exam in minutes                    |
| \(15\)         | Column displaying the total number of items included in the exam                      |
| \(16\)         | Column displaying the current lifecycle status of the exam                            |
| \(17\)         | Action links for each row                                                             |

#### 3.2.3 Config Exam and Exercise Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image38.png" style="width:6.28438in;height:3.93056in" />

This screen allows the **Manager** to:

- **Configure Basic Exam Settings**: set up basic exam information including exam name, exam type, target JLPT level, duration, passing score percentage, and availability status.

- **Define Scoring and Unlock Rules**: specify question distribution across different language skills (Vocabulary / Grammar, Reading, Listening) and set progression behaviors upon passing or failing.

- **Save Configuration**: click the Save Config button to store the entered exam settings.

- **Preview Exam**: click the Preview Exam button to view the exam layout and structure from a student's perspective.

- **Apply Settings**: click the Apply Settings button to activate and publish all configured settings to the exam.

On the screen, s/he can also:

- **View Current User Profile**: view the logged-in Manager's name and role in the top-right corner.

- **Navigate Modules**: switch to other management sections (Dashboard, Course Management, Question Bank, Exam Management, Reports & Analytics) via the left navigation menu.

| **Field Name** | **Description**                                                                                                                                     |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Global text input box located in the top header to search system data                                                                               |
| \(2\)          | Text input field to enter or modify the title of the exam                                                                                           |
| \(3\)          | Dropdown/Select field to choose the type of the exam (e.g., JLPT Mock)                                                                              |
| \(4\)          | Dropdown/Select field to set the target JLPT level (e.g., N3)                                                                                       |
| \(5\)          | Numeric input field to specify the allowed time for completing the exam in minutes                                                                  |
| \(6\)          | Numeric input field to set the minimum percentage required to pass the exam                                                                         |
| \(7\)          | Radio button options to define the exam publishing status (Published, Draft, Scheduled)                                                             |
| \(8\)          | Non-editable list showing the allocation of question quantities across skill categories (Vocabulary / Grammar, Reading, Listening)                  |
| \(9\)          | Checkbox options to set system rules after exam completion (Unlock next module when passed, Recommend review when failed, Allow retake immediately) |
| \(10\)         | Action button to save the current configuration draft                                                                                               |
| \(11\)         | Action button to launch a preview mode of the configured exam                                                                                       |
| \(12\)         | Action button to enforce and apply the configured rules to the exam                                                                                 |

### 3.3 Reports & Analytics 

#### 3.3.1 View Reports Screen (bao gồm Export Reports) 

<img src="Report-3_Software_Requirement_Specification_media/media/image32.png" style="width:6.28438in;height:3.91667in" />

This screen allows the **Manager** to:

- **View Key Analytics Metrics**: view system-wide performance KPIs including total exam attempts, overall average score, course completion rate, and count of at-risk learners.

- **Filter Analytics Data**: adjust reporting parameters by Period (e.g., Last 30 days), JLPT Level (e.g., All), and Content category (e.g., All).

- **View Exam Score Trend**: observe historical performance and score fluctuations over time through graphical trend visualization.

- **Identify Weak Areas**: view systemic weak skill domains among learners, categorized by language components (Grammar, Vocabulary, Reading, Kanji).

- **Export Analytics Reports**: click the **Export Reports** button to download performance reports in supported file formats (CSV/PDF).

On the screen, s/he can also:

- **View Export Scope Details**: review footer summary notes detailing available export formats and included report sections (summary, level breakdown, and question analysis).

- **View Current User Profile**: view the logged-in Manager's name and role in the top-right corner.

- **Navigate Modules**: switch to other management sections (Dashboard, Course Management, Question Bank, Exam Management, Reports & Analytics) via the left navigation menu.

| **Field Name** | **Description**                                                                                                                   |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Global text input box located in the top header to search system data                                                             |
| \(2\)          | Option parameter specifying the evaluation timeframe for generated analytics (e.g., Last 30 days)                                 |
| \(3\)          | Dropdown option to filter reporting metrics by target JLPT level                                                                  |
| \(4\)          | Dropdown option to filter reporting metrics by specific content categories                                                        |
| \(5\)          | Action button to export generated analytics data                                                                                  |
| \(6\)          | Metric card showing total exam submissions and comparison percentage against the prior period                                     |
| \(7\)          | Metric card showing average performance score calculated across all levels                                                        |
| \(8\)          | Metric card showing the percentage of learners completing their assigned learning paths                                           |
| \(9\)          | Metric card showing the total count of learners requiring review recommendations or extra support                                 |
| \(10\)         | Container reserved for displaying performance score trend line charts                                                             |
| \(11\)         | Display section presenting weak performance distribution percentages across language skills (Grammar, Vocabulary, Reading, Kanji) |
| \(12\)         | Informational text footer displaying supported export formats (CSV/PDF) and report contents                                       |

## 4. Admin Features 

### 4.1 System Administration 

#### 4.1.1 View User Screen (User Management)

<img src="Report-3_Software_Requirement_Specification_media/media/image24.png" style="width:6.28438in;height:3.63889in" />

This screen allows the Admin to:

- View the list of registered users, including their IDs, names, email addresses, roles, account statuses, and creation dates.

- Search for users by name or email address.

- Filter users by role, including Learner, Manager, and Admin.

- Filter users by account status.

- View detailed information about a selected user.

- Update user information, roles, and account statuses.

- Navigate through multiple pages of user records and view the total number of users.

**Field Description**

| **Field Name** | **Description**                                                                                                    |
|----------------|--------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Admin to search for users by name or email address and filter the user list by role and account status. |
| \(2\)          | Allows the Admin to view detailed information about the selected user.                                             |
| \(3\)          | Allows the Admin to open the user information form to update the selected user.                                    |
| \(4\)          | Allows the Admin to navigate between pages of user records and view different sets of users.                       |

#### 4.1.2 View User Detail Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image26.png" style="width:6.28438in;height:3.63889in" />

This screen allows the Admin to:

- View the selected user's profile information, including their avatar, full name, email address, and account status.

- Navigate between the user's Information, Learning Process, and Order History sections.

- View the user's personal information, including full name, email address, phone number, current JLPT level, target JLPT level, role, and account status.

- Review the selected user's information without editing it.

- Return to the User List page.

**Field Description**

| **Field Name** | **Description**                                                                                                                                                                                                       |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Displays the selected user's personal information, including full name, email address, phone number, current JLPT level, target JLPT level, role, and account status. The information is presented in read-only mode. |

#### 4.1.3 Update User Screen

<img src="Report-3_Software_Requirement_Specification_media/media/image18.png" style="width:6.28438in;height:3.625in" />

This screen allows the Admin to:

- View the selected user's profile information, including their name, email address, avatar, and account status.

- Navigate between the user's Information, Learning Process, and Order History sections.

- View the user's personal information, including full name, email address, phone number, current JLPT level, and target JLPT level.

- Update the user's role, such as Learner, Manager, or Admin.

- Change the user's account status.

- Save the updated user information or cancel the editing process. Return to the User List page.

**Field Description**

| **Field Name** | **Description**                                                                                             |
|----------------|-------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Admin to change the selected user's role within the JEPS system.                                 |
| \(2\)          | Allows the Admin to change the user's account status.                                                       |
| \(3\)          | Allows the Admin to save the updated user information or cancel the editing process without saving changes. |

#### 4.1. Monitor System Log Screen (System Audit Log)

<img src="Report-3_Software_Requirement_Specification_media/media/image25.png" style="width:6.28438in;height:3.61111in" />

This screen allows the Admin to:

- View system logs recording activities performed by users, including Admins, Managers, and Learners.

- Filter system logs by a specified date range.

- Filter logs by action type, such as adding, updating, deleting, or logging in.

- Export system log data for reporting and auditing purposes. Review log details, including the timestamp, user, action, affected entity, and description of each activity.

- Navigate between pages of system log records and view the total number of logs

**Field Description**

| **Field Name** | **Description**                                                                                                       |
|----------------|-----------------------------------------------------------------------------------------------------------------------|
| \(1\)          | Allows the Admin to filter system logs by date range and action type, and export the filtered log data.               |
| \(2\)          | Displays system activity records, including the time, user, action type, affected entity, and details of each action. |
| \(3\)          | Allows the Admin to navigate between pages of system log records and view different sets of logs.                     |

#### 

## 5. Background Job Inventory

### 5.1 Send Automated Notifications (JOB-01) 

***Trigger***: Automatic — when an event requiring notification occurs or when the configured notification time is reached.

***Purpose***: Automatically send notifications to Learners related to the learning process, courses, assessments, results, or other necessary events in JEPS.

***Processing steps:***

1.  Check notifications waiting to be sent or events that require notification creation.

2.  Identify the Learner who will receive the notification.

3.  Create notification content corresponding to the event.

4.  Save the notification in the JEPS system.

5.  Send the notification to the Learner.

6.  Update the notification sending status.

7.  Log the processing result.

***Failure behavior*:**

| **Scenario**                     | **System behavior**                                                                            |
|----------------------------------|------------------------------------------------------------------------------------------------|
| Unable to create notification    | The system logs the error and does not mark the notification as sent.                          |
| Notification sending fails       | The system keeps the notification in unsent status and may retry in the next processing cycle. |
| No notifications need to be sent | The system ends without performing any further action. This is a normal case.                  |

### 5.2 Update Learning Path After Final Assessment (JOB-02) 

***Trigger***: Event-based — after the Learner completes an Assessment, the system stores the submission, and AI retrieves the data after the Placement Assessment has been graded.

***Purpose***: Automatically use the assessment result to re-analyze the Learner’s ability and update the appropriate Learning Path through Gemini.

***Processing steps:***

1.  Receive the graded assessment result.

2.  Retrieve data related to the Learner and the current Learning Path.

3.  Prepare the necessary data to send to Gemini.

4.  Send the assessment result and learning data to the Gemini API.

5.  Receive the analysis result and proposed Learning Path.

6.  Update the Learner’s Learning Path in JEPS.

7.  Save the Learning Path change information.

8.  Create a notification for the Learner about the new Learning Path.

**Failure behavior:**

| **Scenario**                                     | **System behavior**                                                              |
|--------------------------------------------------|----------------------------------------------------------------------------------|
| Gemini API does not respond                      | The system logs the error and keeps the current Learning Path unchanged.         |
| Gemini returns invalid data                      | The system does not update the Learning Path and records the error for handling. |
| Assessment result cannot be found                | The system stops processing for that Learner and logs a warning.                 |
| Updating the Learning Path in the database fails | The current Learning Path is not overwritten; the system logs the error.         |

### 5.3 Aggregate Learning Report Data (JOB-03) 

***Trigger***: Scheduled — runs automatically according to the report aggregation schedule configured in the system.

***Purpose***: Automatically aggregate Learner learning and activity data to support the Reports and Learning Process functions.

***Processing steps:***

1.  Determine the time period of data to be aggregated.

2.  Retrieve data related to the Learner’s learning process.

3.  Aggregate data such as learning progress, assessment results, JLPT Mock results, and learning activities.

4.  Calculate the necessary summary metrics for reports.

5.  Save the aggregated results in the system.

6.  Update data for Reports and Learning Process.

7.  Log the result of the aggregation process.

**Failure behavior:**

| **Scenario**                | **System behavior**                                                                                 |
|-----------------------------|-----------------------------------------------------------------------------------------------------|
| Database cannot be accessed | The system stops processing and logs the error. Previous report data remains unchanged.             |
| No new data                 | The system ends without updating the report. This is a normal case.                                 |
| Some data is invalid        | The system skips the invalid data, logs a warning, and continues processing valid data if possible. |

## 

### 5.4 Send Automated Email (JOB-04) 

***Trigger***: Event-based — when the system generates an event that requires an Email to be sent.

***Purpose***: Automatically send Email to Learners when important events occur, such as learning notifications, assessment results, Learning Path updates, or confirmations related to courses and payments.

***Processing steps:***

1.  Receive an Email sending request from the system.

2.  Identify the recipient’s Email address.

3.  Create Email content corresponding to the event.

4.  Send the Email through the Email Service.

5.  Receive the sending result from the Email Service.

6.  Update the Email sending status.

7.  Log the sending result.

***Processing steps:***

1.  Receive an Email sending request from the system.

2.  Identify the recipient’s Email address.

3.  Create Email content corresponding to the event.

4.  Send the Email through the Email Service.

5.  Receive the sending result from the Email Service.

6.  Update the Email sending status.

7.  Log the sending result.

**Failure behavior:**

| **Scenario**                 | **System behavior**                                               |
|------------------------------|-------------------------------------------------------------------|
| No valid Email address       | The system does not send the Email and logs a warning.            |
| Email Service is unavailable | The system logs the error and keeps the request in unsent status. |
| Email sending fails          | The system does not mark the Email as sent and may retry later.   |

## 

## 6. External API Inventory

### 6.1 Gemini API — AI Learning Path (API-01) 

| ***URL***     | **POST: Gemini API endpoint**                                                                                                      |
|---------------|------------------------------------------------------------------------------------------------------------------------------------|
| ***Purpose*** | Send assessment result data and learning information to Gemini to generate or update a personalized Learning Path for the Learner. |
| ***Caller***  | JEPS                                                                                                                               |
| ***Auth***    | Yes — Gemini API Key                                                                                                               |

### 6.2 Gemini API — Generate JLPT Mock Test (API-02) 

| ***URL***     | **POST: Gemini API endpoint**                                                                                |
|---------------|--------------------------------------------------------------------------------------------------------------|
| ***Purpose*** | Use Gemini to support the generation of JLPT Mock Exam content based on JEPS requirements and question data. |
| ***Caller***  | JEPS                                                                                                         |
| ***Auth***    | Yes — Gemini API Key                                                                                         |

### 6.3 VNPay Payment API (API-03) 

| ***URL***     | **POST: VNPay Payment API**                                                                                 |
|---------------|-------------------------------------------------------------------------------------------------------------|
| ***Purpose*** | Send payment information from JEPS to VNPay to process payment transactions for courses or Course Packages. |
| ***Caller***  | JEPS                                                                                                        |
| ***Auth***    | Yes — security signature according to VNPay’s mechanism                                                     |

### 6.4 VNPay Payment Webhook / IPN (API-04) 

| ***URL***     | **POST: /api/v1/commerce/payment/webhook/vnpay**                                                                                                                                                 |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ***Purpose*** | Receive payment confirmation results from VNPay and update the payment status in JEPS. After successful payment, the system may create Course Enrollment and grant course access to the Learner. |
| ***Caller***  | VNPay Payment Gateway                                                                                                                                                                            |
| ***Auth***    | Yes — signature verification from VNPay                                                                                                                                                          |

### 6.5 Email Service API (API-05) 

| ***URL***     | **Email Service API / SMTP endpoint**                                                    |
|---------------|------------------------------------------------------------------------------------------|
| ***Purpose*** | Send automated Email from JEPS to Learners when notifications or important events occur. |
| ***Caller***  | JEPS                                                                                     |
| ***Auth***    | Yes — according to the authentication mechanism of the Email Service being used          |

# IV. Non-Functional Requirements

## 1. External Interfaces

## 1.1 User Interface

| **Interface**            | **Description**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Guest Interface          | Allows Guests to access Home Page, view About Us, View Course List, View Course Details, View JLPT Mock Exam, and perform Register Account.                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Learner Interface        | Allows Learners to perform User Login, Reset Password, view and update User Profile, Change Password, access Learner Dashboard, take Placement Assessment Test, Prior Study Check, JLPT Mock Exam, Milestone Exam, take exams in Exam Room, view Exam Result & Solutions, access Course, track Learning Process, Order History, Exam History, Exam List, use Lesson, Flashcard, Study Materials, Exercise Quiz, set up Learning Goal & Hours Setup, generate AI Learning Path, adjust Learning Path Schedule, receive AI Personalized Course, perform Checkout, Payment, and view Invoice. |
| Manager Interface        | Allows Managers to access Manager Dashboard, perform Course Management, Pricing Setup, Learning Lesson Management, Organize Learning Lesson, Exercise Management, Generate Exercise, Question Bank Management, Exam Management for Milestone Exam, JLPT Mock Exam, and Placement Assessment, Generate Exam, Exam and Exercise Configuration, and view Report.                                                                                                                                                                                                                              |
| Admin Interface          | Allows Admins to access Admin Dashboard, perform User Management, view User Details, Update User (Role, Status), and monitor System Audit Log.                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Responsive Web Interface | The JEPS interface must display and function stably on common screen sizes of computers and mobile devices.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |

## 1.2 Software Interfaces

| **External System**           | **Purpose**                                                                                                                          | **Communication**   |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| Gemini API                    | Generate and update the personalized Learning Path based on the Learner’s assessment results, and support JLPT Mock Test generation. | HTTPS / REST API    |
| Payment Service               | Process payments for courses or Course Packages.                                                                                     | HTTPS / Payment API |
| Payment Service Webhook / IPN | Send payment results from Payment Service back to JEPS.                                                                              | HTTPS / Webhook     |
| Google Service                | Send notification emails to Learners.                                                                                                | SMTP or Email API   |

## 1.3 Hardware Interfaces

| **Requirement**  | **Description**                                                                                     |
|------------------|-----------------------------------------------------------------------------------------------------|
| User Devices     | Users only need a device with a web browser and an Internet connection.                             |
| Server           | JEPS is deployed on a server capable of running the Web Application, Backend Service, and Database. |
| Special Hardware | No special hardware devices are required within the current scope of the system.                    |

## 2. Quality Attributes

### 2.1 Usability 

| **Requirement**                    | **Description**                                                                                                                                                      |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Easy Navigation                    | Users must be able to access the main functions through clear and easy-to-understand menus and navigation.                                                           |
| Consistent Interface               | JEPS screens must use consistent layouts, buttons, colors, and function names.                                                                                       |
| Clear Feedback                     | After actions such as Register Account, Login, Submit Assessment, Checkout, or Payment, the System must display clear results or messages to users.                  |
| Input Validation Messages          | When users enter invalid data, the System must display easy-to-understand error messages and clearly indicate which information needs to be corrected.               |
| Access to Learning Features        | Learners must be able to easily access their Learning Path, Courses, Lessons, Assessments, and Learning Results from the learning interface.                         |
| Basic Learning Time                | New Learners must be able to use the basic functions of JEPS without formal training or with only brief instructions.                                                |
| Responsive Interface               | Main screens must be usable on computers and mobile devices through modern web browsers.                                                                             |
| Confirmation for Important Actions | Important actions such as submitting an Exam, making a Payment, or deleting management data must display an appropriate confirmation message before being performed. |

### 2.2 Performance

| **Requirement**                 | **Target**                                                                   | **Condition**                                    |
|---------------------------------|------------------------------------------------------------------------------|--------------------------------------------------|
| Page Load Time                  | Standard pages must load within 2 seconds.                                   | Up to 100 concurrent users.                      |
| Form Submission Response Time   | The System must respond to form submissions within 2 seconds.                | Up to 100 concurrent users.                      |
| Login Response Time             | The System must process Login within 2 seconds.                              | Normal network and Database connection.          |
| Placement Assessment Submission | The System must confirm successful submission within 3 seconds.              | Excluding Gemini analysis time.                  |
| JLPT Mock Exam Submission       | The System must confirm successful submission within 3 seconds.              | Excluding external AI processing time.           |
| AI Learning Path Generation     | AI results must be returned within approximately 10 seconds.                 | Gemini API is operating normally.                |
| Payment Request                 | JEPS must redirect users to the Payment process within 3 seconds.            | VNPay is operating normally.                     |
| View Reports                    | Summary Reports must be displayed within 5 seconds.                          | Summary data is available.                       |
| Maximum Concurrent Users        | The System must maintain its core functions with up to 100 concurrent users. | In the current version's deployment environment. |

### 2.3 Security

*\[State the requirement of security, not the implementation mechanism. Some samples are as below\]*

| **Requirement**                   | **Description**                                                                                                                                                                                                        |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authentication Required           | JEPS personal and management functions require users to log in. Public content such as Home Page, About Us, Course List, Course Details, View JLPT Mock Exam, and Register Account can be accessed without logging in. |
| Assessment Authentication         | Learners must log in before taking the Placement Assessment or JLPT Mock Exam.                                                                                                                                         |
| Password Protection               | Passwords must not be stored or displayed in a form that can be directly read by a person or program.                                                                                                                  |
| Login Failure Handling            | JEPS allows users to retry logging in when incorrect information is entered. Currently, the system does not lock accounts based on the number of failed login attempts.                                                |
| Role-Based Access Control         | Users can only access functions and data that match their assigned roles: Guest, Learner, Manager, or Admin.                                                                                                           |
| Learner Data Protection           | Learners can only access their own Learning Path, Assessment Results, Orders, and personal data.                                                                                                                       |
| Manager Access Control            | Course, Learning Lesson, Exercise, Question Bank, Exam, Exam and Exercise Configuration, and Report management functions can only be accessed by users with the appropriate Manager permissions.                       |
| Admin Access Control              | User Management, User Details, Update User (Role, Status), and System Audit Log functions can only be accessed by authorized Admins.                                                                                   |
| Payment Data Protection           | JEPS must not directly trust payment data submitted by users. Payment results must be confirmed through VNPay before granting access to a Course.                                                                      |
| External API Security             | Login details and secret keys used to connect to Gemini, VNPay, and Email Service must be protected and must not be exposed to end users.                                                                              |
| Secure Communication              | Sensitive data exchanged between JEPS and external services must be sent through secure connections.                                                                                                                   |
| Access Confirmation After Payment | Course Enrollment and Course access rights are only activated after the System confirms successful Payment.                                                                                                            |
| Input Validation                  | Data entered by users must be checked for validity before being processed or stored.                                                                                                                                   |
| Logging and Tracking              | Administration activities and important System events must be able to be recorded for monitoring in the System Audit Log.                                                                                              |

# V. Requirement Appendix

## 1. Business Rules

| **GBR-ID** | **Rule Statement**                                                                                                                                                                                                                                           | **Applies To**                                                                       | **Rationale**                                                                                 | **Enforced By**                                                                                               |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| GB-01      | Users must log in before taking the Placement Assessment.                                                                                                                                                                                                    | Placement Assessment                                                                 | Ensure that assessment results are correctly linked to the Learner to create a Learning Path. | The System checks the login status before starting the assessment.                                            |
| GB-02      | Each account must use a valid email address that is not already used by another account.                                                                                                                                                                     | Register Account, User Management                                                    | Ensure that each account is unique and supports login and password recovery.                  | The System checks the email when registering or updating an account.                                          |
| GB-03      | Passwords must meet the minimum security requirements set by the System.                                                                                                                                                                                     | Register, Change Password, Reset Password                                            | Reduce the risk of unauthorized account access.                                               | The System checks the password format before saving it.                                                       |
| GB-04      | When login fails, the System must not reveal whether the email exists or the password is incorrect.                                                                                                                                                          | Login                                                                                | Prevent attackers from identifying valid accounts.                                            | The System displays the same general error message for all failed login attempts.                             |
| GB-05      | Disabled or locked accounts must not be allowed to log into the System.                                                                                                                                                                                      | Login, User Management                                                               | Ensure that invalid accounts cannot continue accessing the System.                            | The System checks the account status during login verification.                                               |
| GB-06      | The Placement Assessment must be fully graded before the System sends data to AI to create a Learning Path.                                                                                                                                                  | Placement Assessment, Learning Path                                                  | AI needs valid assessment data to create a suitable Learning Path.                            | The System checks the Assessment Result before calling the AI System.                                         |
| GB-07      | The Learning Path must be created based on at least the Placement Assessment Result, AI analysis of the initial assessment, the Course purchased by the Learner, the time the Learner can study each day, and the Learner's learning information/JLPT goals. | Personalized Learning Path                                                           | Ensure that the Learning Path is created based on the Learner's actual data.                  | The System and AI System work together to process the input data.                                             |
| GB-08      | Learners can only view the Learning Paths belonging to their own accounts.                                                                                                                                                                                   | Learning Path                                                                        | Protect personal learning data.                                                               | The System checks user_id for each Learning Path access request.                                              |
| GB-09      | Learners can only access the full content of a paid Course after the Order has been confirmed as successfully paid.                                                                                                                                          | Course, Checkout, Payment, Enrollment                                                | Prevent access to paid content before the transaction is completed.                           | The System checks Order Status and Payment Status before activating Enrollment.                               |
| GB-10      | A Course that the Learner already owns or has successfully enrolled in cannot be added to the cart again for another purchase.                                                                                                                               | Course Enrollment                                                                    | Prevent duplicate Orders.                                                                     | The System checks Enrollment/Order.                                                                           |
| GB-11      | Only Courses with a status that allows Enrollment can proceed to Checkout.                                                                                                                                                                                   | Course, Checkout                                                                     | Prevent the purchase of Courses that are no longer available.                                 | The System checks Course Status before Payment.                                                               |
| GB-12      | The total Order amount must be recalculated by the System during Checkout and must not directly use the amount sent by the client.                                                                                                                           | Checkout, Order                                                                      | Prevent users from changing prices or Payment data on the client side.                        | The Server recalculates subtotal, discount, and total before creating the Order.                              |
| GB-13      | An Order can only be changed to Paid status when the Payment Service returns a valid and successful Payment result.                                                                                                                                          | Payment, Order                                                                       | Ensure that the Order Status correctly reflects the actual transaction.                       | The System verifies the response from the Payment Service.                                                    |
| GB-14      | Learners are not allowed to change Payment Status or Order Status themselves.                                                                                                                                                                                | Payment, Order Management                                                            | Protect the accuracy and integrity of transaction data.                                       | Only the System/Payment Service or Administrator can update these statuses according to the business process. |
| GB-15      | An Invoice can only be created or displayed for an Order that has been successfully paid.                                                                                                                                                                    | Invoice, Order                                                                       | Ensure that each Invoice is linked to a valid transaction.                                    | The System checks Order Status and Payment Status before displaying the Invoice.                              |
| GB-16      | Exercise is only a practice activity, and Practice Results must not be considered official JLPT results.                                                                                                                                                     | Exercise Result                                                                      | Distinguish internal practice activities from the actual JLPT exam.                           | The System clearly displays the result type.                                                                  |
| GB-17      | Learners can only take the Course Final Assessment when they meet the configured Course completion requirements.                                                                                                                                             | Final Assessment                                                                     | Ensure that the Final Assessment is taken after completing the required learning process.     | The System checks Course Progress before allowing the Learner to start the assessment.                        |
| GB-18      | The Final Assessment Result is used to determine whether the Learner meets the requirements to complete the Course.                                                                                                                                          | Final Assessment, Course Completion                                                  | Ensure that Course Completion Status is determined consistently.                              | The System compares the result with the configured completion threshold.                                      |
| GB-19      | If the Learner PASSES the Final Assessment, the System may move the Learning Path to the next level or learning stage.                                                                                                                                       | Final Assessment, Learning Path                                                      | Help the Learner continue learning at a suitable pace.                                        | The System/AI System updates the Learning Path after evaluating the result.                                   |
| GB-20      | If the Learner DOES NOT PASS the Final Assessment, the System recommends continuing to study or reviewing suitable content before taking the assessment again.                                                                                               | Final Assessment, Learning Path                                                      | Focus on improving knowledge areas that have not met the requirements.                        | The AI System analyzes the result and updates learning recommendations.                                       |
| GB-21      | Final Assessment Results are only internal assessments of JEPS and must not be presented as official JLPT results.                                                                                                                                           | Final Assessment Result                                                              | Avoid confusion with official JLPT certification.                                             | The interface and reports must clearly state that these are internal results.                                 |
| GB-22      | The JLPT Mock Exam is an independent practice feature and does not automatically create or update a Learning Path.                                                                                                                                           | JLPT Mock Exam, Mock Exam Result                                                     | Separate mock exam practice from Placement Assessment and Course Final Assessment.            | The System does not trigger Generate/Update Learning Path after a Mock Exam.                                  |
| GB-23      | The Mock Exam Result must display at least Score, Correct Answers, Incorrect Answers, and Percentage when the data is available.                                                                                                                             | Mock Exam Result                                                                     | Provide clear practice results for users.                                                     | The System summarizes the results after the Mock Exam is submitted.                                           |
| GB-24      | Mock Exam Results must not be presented as official JLPT exam results.                                                                                                                                                                                       | Mock Exam Result                                                                     | A Mock Exam is only a simulation of the JLPT exam.                                            | The System displays a clear note on the result screen.                                                        |
| GB-25      | An Assessment Exam can only be graded after the user submits the exam or the configured exam time expires.                                                                                                                                                   | Placement Assessment, Milestone Exam, JLPT Mock Exam                                 | Prevent incomplete assessment data from being graded incorrectly.                             | The Assessment Engine checks Exam Status.                                                                     |
| GB-26      | After an Assessment Exam has been submitted, the answers for that attempt cannot be edited.                                                                                                                                                                  | Assessment Exam                                                                      | Ensure the integrity of assessment results.                                                   | The System changes the Exam Status to Submitted/Completed and locks the Answer data.                          |
| GB-27      | Learners can only view Orders, Invoices, Exam Results, and learning data belonging to their own accounts.                                                                                                                                                    | Order, Invoice, Exam, Progress                                                       | Protect personal data and transaction information.                                            | The System checks data ownership for each access request.                                                     |
| GB-28      | Administrators can only perform functions that match their assigned roles and permissions.                                                                                                                                                                   | Administration, User Management, Configuration                                       | Prevent unauthorized access to administration features.                                       | The System applies Role-Based Access Control.                                                                 |
| GB-29      | Important administration actions must be recorded in the Audit Log.                                                                                                                                                                                          | User Management, Role Management, System Configuration, Payment/Order Administration | Support reviewing change history and tracking problems.                                       | The System automatically records logs when actions are completed.                                             |
| GB-30      | Data sent to the AI System must not directly change the original Exam data.                                                                                                                                                                                  | AI Learning Path                                                                     | Preserve the original assessment data for checking and tracking purposes.                     | The System only sends a copy of the necessary data to AI and stores AI results separately.                    |

## 2. System Messages

| **\#** | **Message Code** | **Message Type**       | **Context**                                                | **Content**                                                                                                         |
|--------|------------------|------------------------|------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| 1      | MSG01            | Inline                 | No search data                                             | No matching results found.                                                                                          |
| 2      | MSG02            | Error under field      | Required field is left blank                               | This field is required.                                                                                             |
| 3      | MSG03            | Error under field      | Invalid email format                                       | Invalid email. Please check again.                                                                                  |
| 4      | MSG04            | Error under field      | Registered email already exists                            | This email has already been used.                                                                                   |
| 5      | MSG05            | Toast – Success        | Account registration successful                            | Account registration successful.                                                                                    |
| 6      | MSG06            | Inline / Toast – Error | Login failed                                               | Incorrect email or password. Please check again.                                                                    |
| 7      | MSG07            | Error / Dialog         | Account is locked                                          | The account is currently locked. Please try again later or contact the administrator.                               |
| 8      | MSG08            | Error / Dialog         | Account has been disabled                                  | This account has been disabled.                                                                                     |
| 9      | MSG09            | Toast – Success        | Login successful                                           | Login successful.                                                                                                   |
| 10     | MSG10            | Error under field      | Value exceeds maximum length                               | Content must not exceed {max_length} characters.                                                                    |
| 11     | MSG11            | Confirmation Dialog    | User is about to start Placement Assessment                | Are you sure you want to start the placement assessment?                                                            |
| 12     | MSG12            | Warning                | Learner is not logged in but accesses Placement Assessment | Please log in to take the placement assessment.                                                                     |
| 13     | MSG13            | Confirmation Dialog    | Submit Assessment                                          | Are you sure you want to submit the assessment? After submission, you cannot change your answers.                   |
| 14     | MSG14            | Toast – Success        | Submit Assessment successful                               | Assessment submitted successfully.                                                                                  |
| 15     | MSG15            | Error                  | Submit Assessment failed                                   | Unable to submit the assessment. Please try again.                                                                  |
| 16     | MSG16            | Loading / Inline       | AI is generating Learning Path                             | The system is analyzing the results and creating your learning path.                                                |
| 17     | MSG17            | Error                  | AI cannot generate Learning Path                           | Unable to create the learning path at this time. Please try again later.                                            |
| 18     | MSG18            | Toast – Success        | Learning Path created                                      | Your learning path has been created successfully.                                                                   |
| 19     | MSG21            | Warning                | Learner already owns Course                                | You have already enrolled in this course.                                                                           |
| 20     | MSG24            | Error                  | Cart is empty during Checkout                              | Your cart is empty.                                                                                                 |
| 21     | MSG25            | Loading / Inline       | Redirecting to Payment Service                             | Connecting to the payment service. Please wait.                                                                     |
| 22     | MSG26            | Success Page / Toast   | Payment successful                                         | Payment successful.                                                                                                 |
| 23     | MSG27            | Error Page / Toast     | Payment failed                                             | Payment was unsuccessful. Please try again.                                                                         |
| 24     | MSG28            | Warning                | Payment canceled                                           | The payment transaction has been canceled.                                                                          |
| 25     | MSG29            | Warning                | Payment pending                                            | The transaction is being processed. Please check again later.                                                       |
| 26     | MSG30            | Toast – Success        | Enrollment activated                                       | You have successfully enrolled in the course.                                                                       |
| 27     | MSG31            | Warning                | Learner is not eligible for Final Assessment               | You have not met the requirements to take the final assessment.                                                     |
| 28     | MSG32            | Confirmation Dialog    | Start Final Assessment                                     | Are you sure you want to start the final assessment?                                                                |
| 29     | MSG33            | Result Message         | Learner PASS Final Assessment                              | You have completed the course. The next learning path is being updated.                                             |
| 30     | MSG34            | Result Message         | Learner NOT PASS Final Assessment                          | You have not met the completion requirements. The system will recommend content that you need to continue studying. |
| 31     | MSG35            | Information            | Final Assessment Result                                    | This is an internal JEPS assessment result, not an official JLPT result.                                            |
| 32     | MSG36            | Confirmation Dialog    | Start Mock Exam                                            | Are you sure you want to start this JLPT Mock Exam?                                                                 |
| 33     | MSG37            | Confirmation Dialog    | Submit Mock Exam                                           | Are you sure you want to submit the Mock Exam?                                                                      |
| 34     | MSG38            | Information            | Mock Exam Result                                           | The Mock Exam result is for practice purposes only and does not automatically update the Learning Path.             |
| 35     | MSG39            | Information            | Mock Exam Result                                           | This is not an official JLPT result.                                                                                |
| 36     | MSG40            | Toast – Success        | Practice Exercise completed                                | You have completed the practice exercise.                                                                           |
| 37     | MSG41            | Error                  | Unable to load Practice/Assessment data                    | Unable to load data. Please try again.                                                                              |
| 38     | MSG42            | Toast – Success        | Profile updated successfully                               | Personal information updated successfully.                                                                          |
| 39     | MSG43            | Toast – Success        | Administrator updates data                                 | Data updated successfully.                                                                                          |
| 40     | MSG44            | Confirmation Dialog    | Administrator deletes/disables data                        | Are you sure you want to perform this action?                                                                       |
| 41     | MSG45            | Error                  | Access denied                                              | You do not have permission to access this function.                                                                 |
| 42     | MSG46            | Error                  | Session expired                                            | Your login session has expired. Please log in again.                                                                |
| 43     | MSG47            | Error                  | General system error                                       | An error occurred. Please try again later.                                                                          |
| 44     | MSG48            | Error                  | Network connection lost                                    | Unable to connect to the system. Please check your network connection.                                              |

## 3. Other Requirements

…
