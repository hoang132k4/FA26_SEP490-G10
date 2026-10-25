<img src="Report-2_Project_Management_Plan_media/media/image1.png" style="width:2.961in;height:0.90999in" />

**<span class="smallcaps">CAPSTONE PROJECT REPORT</span>**

**Report 2 – Project Management Plan**

> – Hanoi, Aug 2026 –

**Table of Contents**

[Change Log 3](#change-log)

[I. Project Overview 4](#i.-project-overview)

> [1. Scope and Purpose 4](#scope-and-purpose)
>
> [2. Assumptions & Constraints 4](#assumptions-constraints)
>
> [3. Cost & Time Estimations 4](#cost-time-estimations)
>
> [4. Capacity Planning 6](#capacity-planning)
>
> [5. Project Objectives 6](#project-objectives)
>
> [6. Risks and Mitigations 6](#risks-and-mitigations)

[II. Management Approach 7](#ii.-management-approach)

> [1. Project Process/Methodology 7](#project-processmethodology)
>
> [2. Quality Management 7](#quality-management)
>
> [3. Project Training Plan 7](#project-training-plan)
>
> [4. Responsibility Assignments 7](#responsibility-assignments)
>
> [5. Project Communications 8](#project-communications)
>
> [6. Configuration Management 8](#configuration-management)
>
> [6.1 Document Management 8](#document-management)
>
> [6.2 Source Code Management 8](#source-code-management)
>
> [6.3 Tools & Infrastructures 8](#tools-infrastructures)

# Change Log

<table>
<colgroup>
<col style="width: 13%" />
<col style="width: 7%" />
<col style="width: 13%" />
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
<th>11/09/2026</th>
<th>A</th>
<th>HungNH</th>
<th>Project Overview</th>
</tr>
<tr class="header">
<th>20/09/2026</th>
<th>M</th>
<th>HoangHH</th>
<th>Project Objectives , Capacity Planning , Risks and Mitigations ,Assumptions and Constraints</th>
</tr>
<tr class="odd">
<th>20/09/2026</th>
<th>M</th>
<th>HungNH</th>
<th><p>Project Overview( 1.1 Purpose, 1.2 Project Scope, 1.3 Out of Scope)</p>
<p>Management Approach(1. Project Process/Methodology, 2. Quality Management, 3. Project Training Plan, 5. Project Communications, 6. Configuration Management)</p></th>
</tr>
<tr class="header">
<th>20/09/2026</th>
<th>M</th>
<th>HoangNT</th>
<th><p>I. Project Overview(2. Assumptions &amp; Constraints)</p>
<p>II. Management Approach(5. Project Communications)</p></th>
</tr>
<tr class="odd">
<th>20/09/2026</th>
<th>M</th>
<th>MinhPD</th>
<th><p>I.2.Assumptions and Constraints</p>
<p>I.6. Risks and Mitigations</p>
<p>II.4. Responsibility Assignments</p></th>
</tr>
<tr class="header">
<th>20/09/2026</th>
<th>M</th>
<th>LongBM</th>
<th>Project Objectives , Capacity Planning , Risks and Mitigations ,Assumptions and Constraints</th>
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

# I. Project Overview

## 1. Scope and Purpose

### 1.1 Purpose

The purpose of the JLPT Exam Preparation System is to help learners prepare for JLPT exams from N5 to N1 through personalized learning methods and courses from N5 to N1. The system assesses learners' current proficiency levels, creates suitable learning paths, recommends courses, tracks learning progress, and provides mock exams. Based on the results, AI evaluates and provides learning paths that can be adjusted to better meet each learner's needs.

### 1.2 Project Scope

The JLPT Exam Preparation System – JEPS (Version 1) is developed to support self-study and JLPT exam preparation from N5 to N1. The system scope includes the following feature groups:

- Account and User Management: Registration, login, password reset, viewing/updating profiles, and user management.

- Initial Proficiency Assessment: Allows learners to select a level, take an assessment, and view results.

- Course and Learning Path Recommendations: AI recommends suitable courses and creates and adjusts learning paths based on assessment results and learning progress.

- Course Enrollment and Payment: Course enrollment, payment, viewing order history, order details, and invoices.

- Learning Activities: Studying lessons, practicing with Flashcards, and completing Quizzes/Exercises.

- JLPT Exam and Mock Exams: Taking Milestone Exams and JLPT Mock Exams, viewing results and AI analysis, answers, and exam history.

- Learning Progress Tracking: Tracking learning paths, learning results, and learner progress.

- Content and Exam Management: Managers manage courses, lessons, question banks, practice exercises, and exams.

- Reports and System Administration: Viewing/exporting reports, managing users, and monitoring system logs.

### 1.3 Out of Scope

The following features are outside the scope of version v1:

- Speaking practice features.

- Mobile applications for Android or iOS.

- Dictionary lookup features.

- Note-taking features.

- Features not defined in the project scope or the approved SRS document.

- Features classified as "Future Development" may be considered for later versions but will not be included in the deliverables of the current project.

## 2. Assumptions & Constraints

| **No**      | **Description**                                                                                                                                                                                                                                                                                                               | **Note**   |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| Assumptions |                                                                                                                                                                                                                                                                                                                               |            |
| 1           | Japanese language knowledge data will be available from suitable and reliable sources and provided in a structured format to make data entry and processing in the system easier.                                                                                                                                             | Data       |
| 2           | Suitable AI services will be available for integration into the system. AI-generated content is expected to match the input data and predefined templates and can be checked or reviewed when necessary.                                                                                                                      | Technology |
| 3           | The system is developed to support Japanese self-study and is not intended to completely replace traditional Japanese learning methods.                                                                                                                                                                                       | Scope      |
| Constraints |                                                                                                                                                                                                                                                                                                                               |            |
| 1           | The system must maintain a complete question bank covering JLPT levels from N5 to N1, different knowledge topics, and difficulty levels to ensure the reliability of assessments and mock exams. All learning materials and question data must come from legally licensed sources and follow relevant copyright requirements. | Data       |
| 2           | The system has limitations in speaking practice and JLPT mock exam features. AI-based assessment and personalized recommendations are also limited by the availability and quality of data, as well as the capabilities of the selected AI technology.                                                                        | Technology |
| 3           | Personalized learning paths should be updated based on new assessment results and learners' learning progress. However, the generated recommendations may not always fully reflect learners' actual abilities.                                                                                                                | Quality    |
| 4           | The project has a limited budget for AI integration. Therefore, AI services such as OpenAI ChatGPT and Google Gemini are expected to be used only through free or low-cost API plans. This may lead to limitations in usage volume, available features, response speed, and overall system performance.                       | Budget     |

## 3. Cost & Time Estimations

<table style="width:100%;">
<colgroup>
<col style="width: 8%" />
<col style="width: 67%" />
<col style="width: 10%" />
<col style="width: 12%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>#</strong></th>
<th><strong>Work Package</strong></th>
<th><p><strong>Effort</strong></p>
<p><strong>(pds)</strong></p></th>
<th><strong>Due Date</strong></th>
</tr>
<tr class="odd">
<th><blockquote>
<p><em><strong>1</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Stage 1: Project Initiating (week 1)</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>20</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Week 1</strong></em></p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>1.1</p>
</blockquote></th>
<th><blockquote>
<p>Report 1 (Project Introduction)</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 1</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>1.2</p>
</blockquote></th>
<th><blockquote>
<p>Initial Project Scope and Objectives</p>
</blockquote></th>
<th><blockquote>
<p>6</p>
</blockquote></th>
<th><blockquote>
<p>Week 1</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>1.3</p>
</blockquote></th>
<th><blockquote>
<p>Initial Project Planning</p>
</blockquote></th>
<th><blockquote>
<p>6</p>
</blockquote></th>
<th><blockquote>
<p>Week 1</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p><em><strong>2.</strong></em></p>
</blockquote></th>
<th><em><strong>Stage 2: Project Planning &amp; Initial Requirements (weeks 2-3)</strong></em></th>
<th><blockquote>
<p><em><strong>40</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Weeks 2-3</strong></em></p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>2.1</p>
</blockquote></th>
<th><blockquote>
<p>Report 2: Project Management Plan v1.0</p>
</blockquote></th>
<th><blockquote>
<p>10</p>
</blockquote></th>
<th><blockquote>
<p>Week 2</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>2.2</p>
</blockquote></th>
<th><blockquote>
<p>Report 3: SRS v0.9 – Overall Requirements</p>
</blockquote></th>
<th><blockquote>
<p>15</p>
</blockquote></th>
<th><blockquote>
<p>Week 2</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>2.3</p>
</blockquote></th>
<th><blockquote>
<p>User Requirements Analysis</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 3</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>2.4</p>
</blockquote></th>
<th><blockquote>
<p>Functional Requirements Definition</p>
</blockquote></th>
<th><blockquote>
<p>7</p>
</blockquote></th>
<th><blockquote>
<p>Week 3</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p><em><strong>3</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Stage 3: Software Design (weeks 4-5)</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>40</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Weeks 4-5</strong></em></p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>3.1</p>
</blockquote></th>
<th><blockquote>
<p>Vertical Prototypes (POC, or Proof Of Concepts)</p>
</blockquote></th>
<th><blockquote>
<p>6</p>
</blockquote></th>
<th><blockquote>
<p>Week 4</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>3.2</p>
</blockquote></th>
<th><blockquote>
<p>Report 4: SDS v1.0 (High Level Design Specifications)</p>
</blockquote></th>
<th><blockquote>
<p>12</p>
</blockquote></th>
<th><blockquote>
<p>Week 4</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>3.3</p>
</blockquote></th>
<th><blockquote>
<p>Report 5: Test Planning (ref Report5_Test Documentation)</p>
</blockquote></th>
<th><blockquote>
<p>7</p>
</blockquote></th>
<th><blockquote>
<p>Week 5</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>3.4</p>
</blockquote></th>
<th><blockquote>
<p>Report 3: SRS v1.0 (Detailed Requirements for Iteration 1)</p>
</blockquote></th>
<th><blockquote>
<p>7</p>
</blockquote></th>
<th><blockquote>
<p>Week 5</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>3.5</p>
</blockquote></th>
<th><blockquote>
<p>Database Design</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 5</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p><em><strong>4</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Stage 4: Implementation (weeks 6-11)</strong></em></p>
<p>(Ref: <a href="about:blank"><u>Project Tracking</u></a>, WBS sheet)</p>
</blockquote></th>
<th><blockquote>
<p><em><strong>120</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Weeks 6-11</strong></em></p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p><u>4.1</u></p>
</blockquote></th>
<th><blockquote>
<p><u>Implementation Iteration 1 (weeks 6-7)</u></p>
</blockquote></th>
<th><blockquote>
<p><u>40</u></p>
</blockquote></th>
<th><blockquote>
<p><u>Week 6</u></p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4.1.1</p>
</blockquote></th>
<th><blockquote>
<p>Authentication &amp; Authorization</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 6</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>4.1.2</p>
</blockquote></th>
<th><blockquote>
<p>User &amp; Role Management</p>
</blockquote></th>
<th><blockquote>
<p>6</p>
</blockquote></th>
<th><blockquote>
<p>Week 7</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4. 1.3</p>
</blockquote></th>
<th><blockquote>
<p>Japanese Knowledge Database</p>
</blockquote></th>
<th><blockquote>
<p>12</p>
</blockquote></th>
<th><blockquote>
<p>Week 7</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>4. 1.4</p>
</blockquote></th>
<th><blockquote>
<p>Dictionary Search</p>
</blockquote></th>
<th><blockquote>
<p>5</p>
</blockquote></th>
<th><blockquote>
<p>Week 7</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4.1.5</p>
</blockquote></th>
<th><blockquote>
<p>Unit &amp; Integration Testing</p>
</blockquote></th>
<th><blockquote>
<p>9</p>
</blockquote></th>
<th><blockquote>
<p>Week 8</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p><u>4.2</u></p>
</blockquote></th>
<th><blockquote>
<p><u>Implementation Iteration 2 (weeks 8-9)</u></p>
</blockquote></th>
<th><blockquote>
<p><u>40</u></p>
</blockquote></th>
<th><blockquote>
<p><u>Week 8</u></p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4.2.1</p>
</blockquote></th>
<th><blockquote>
<p>Learning Path</p>
</blockquote></th>
<th><blockquote>
<p>10</p>
</blockquote></th>
<th><blockquote>
<p>Week 9</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>4.2.2</p>
</blockquote></th>
<th><blockquote>
<p>Learning Modules &amp; Learning Content</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 9</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4. 2.3</p>
</blockquote></th>
<th><blockquote>
<p>Flashcards / Bookmarks / Notes</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 9</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>4. 2.4</p>
</blockquote></th>
<th><blockquote>
<p>Learning Progress Tracking</p>
</blockquote></th>
<th><blockquote>
<p>6</p>
</blockquote></th>
<th><blockquote>
<p>Week 9</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4.2.5</p>
</blockquote></th>
<th><blockquote>
<p>Unit &amp; Integration Testing</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 10</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p><u>4.3</u></p>
</blockquote></th>
<th><blockquote>
<p><u>Implementation Iteration 3 (weeks 10-11)</u></p>
</blockquote></th>
<th><blockquote>
<p><u>40</u></p>
</blockquote></th>
<th><blockquote>
<p><u>Week 10</u></p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4.3.1</p>
</blockquote></th>
<th><blockquote>
<p>Placement Assessment</p>
</blockquote></th>
<th><blockquote>
<p>7</p>
</blockquote></th>
<th><blockquote>
<p>Week 10</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>4.3.2</p>
</blockquote></th>
<th><blockquote>
<p>Practice Quiz</p>
</blockquote></th>
<th><blockquote>
<p>7</p>
</blockquote></th>
<th><blockquote>
<p>Week 10</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4.3.3</p>
</blockquote></th>
<th><blockquote>
<p>JLPT Mock Exam</p>
</blockquote></th>
<th><blockquote>
<p>9</p>
</blockquote></th>
<th><blockquote>
<p>Week 11</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>4.3.4</p>
</blockquote></th>
<th><blockquote>
<p>Performance Analysis</p>
</blockquote></th>
<th><blockquote>
<p>7</p>
</blockquote></th>
<th><blockquote>
<p>Week 11</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>4.3.5</p>
</blockquote></th>
<th><blockquote>
<p>Rule-Based Recommendation</p>
</blockquote></th>
<th><blockquote>
<p>5</p>
</blockquote></th>
<th><blockquote>
<p>Week 11</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>4.3.6</p>
</blockquote></th>
<th><blockquote>
<p>System Testing Preparation</p>
</blockquote></th>
<th><blockquote>
<p>5</p>
</blockquote></th>
<th><blockquote>
<p>Week 11</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p><em><strong>5</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Stage 5: Verification &amp; Validation (weeks 12-14)</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>40</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Weeks 12-14</strong></em></p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>5.1</p>
</blockquote></th>
<th><blockquote>
<p>System Testing</p>
</blockquote></th>
<th><blockquote>
<p>10</p>
</blockquote></th>
<th><blockquote>
<p>Week 12</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>5.2</p>
</blockquote></th>
<th><blockquote>
<p>Integration Testing &amp; Regression Testing</p>
</blockquote></th>
<th><blockquote>
<p>5</p>
</blockquote></th>
<th><blockquote>
<p>Week 12</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>5.3</p>
</blockquote></th>
<th><blockquote>
<p>Acceptance Test Support</p>
</blockquote></th>
<th><blockquote>
<p>7</p>
</blockquote></th>
<th><blockquote>
<p>Week 13</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>5.4</p>
</blockquote></th>
<th><blockquote>
<p>Full Software Package Preparation</p>
</blockquote></th>
<th><blockquote>
<p>8</p>
</blockquote></th>
<th><blockquote>
<p>Week 13</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>5.5</p>
</blockquote></th>
<th><blockquote>
<p>Bug Fixing &amp; System Stabilization</p>
</blockquote></th>
<th><blockquote>
<p>5</p>
</blockquote></th>
<th><blockquote>
<p>Week 14</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>5.6</p>
</blockquote></th>
<th><blockquote>
<p>Report 6: User Guides</p>
</blockquote></th>
<th><blockquote>
<p>5</p>
</blockquote></th>
<th><blockquote>
<p>Week 14</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p><em><strong>6</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Stage 6: Closing (week 15 )</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>15</strong></em></p>
</blockquote></th>
<th><blockquote>
<p><em><strong>Week15</strong></em></p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>6.1</p>
</blockquote></th>
<th><blockquote>
<p>Report 7: Final Project Report</p>
</blockquote></th>
<th><blockquote>
<p>6</p>
</blockquote></th>
<th><blockquote>
<p>Week 15</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>6.2</p>
</blockquote></th>
<th><blockquote>
<p>Final Project Products</p>
</blockquote></th>
<th><blockquote>
<p>4</p>
</blockquote></th>
<th><blockquote>
<p>Week 15</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>6.</p>
</blockquote></th>
<th><blockquote>
<p>Presentation / Defense Slides</p>
</blockquote></th>
<th><blockquote>
<p>5</p>
</blockquote></th>
<th><blockquote>
<p>Week 15</p>
</blockquote></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 4. Capacity Planning

| **Item**                                       | **Value**                           |
|------------------------------------------------|-------------------------------------|
| Hours per person per week                      | \[56\] h (\[8\] h/day × \[7\] days) |
| Team total per week                            | \[5 people × 56\] h/week            |
| Total duration                                 | 14 weeks                            |
| Gross capacity                                 | 350 person-days                     |
| Buffer (meetings, reviews, unplanned — \[8\]%) | 28 person-days                      |
| Effective working capacity                     | 322 person-days                     |

## 5. Project Objectives

| **\#** | **Metric**                   | **Unit**          | **Planned** | **Actual** | **Notes / References**                                                                  |
|--------|------------------------------|-------------------|-------------|------------|-----------------------------------------------------------------------------------------|
| 1      | Effort Usage                 | Person-days       | ≤ 322 PD    | TBD        | JEPS_PM_TaskManagement.xlsx                                                             |
| 2      | Defects Found During Review  | Number of defects | ≤ 15        | TBD        | Backend_Bug_Tracker.xlsx; Frontend Tasks Management.xlsx                                |
| 3      | Unit Testing Defects         | Number of defects | ≤ 20        | TBD        | Tab Test Statistics, Report-5.1_Unit Test.xlsx                                          |
| 4      | Integration Testing Defects  | Number of defects | ≤ 15        | TBD        | Tab Test Statistics, Report-5.2_Integration Test                                        |
| 5      | System Testing Defects       | Number of defects | ≤ 15        | TBD        | Tab Test Statistics, Report-5.3_System Test_FRs (94) & Report-5.4_System_Test_NFRs (11) |
| 6      | Acceptance Testing Defects   | Number of defects | ≤ 10        | TBD        | Report-5.5_Acceptance Test Scripts.xlsx                                                 |
| 7      | On-Time Delivery Rate        | %                 | ≥ 90%       | TBD        | Number of deliverables completed on time / Total number of project deliverables         |
| 8      | Requirements Completion Rate | %                 | ≥ 90%       | TBD        | Percentage of committed requirements that have been implemented and verified            |
| 9      | Critical Defects at Release  | Number of defects | 0           | TBD        | No unresolved critical defects remaining in the final version                           |
| 10     | Test Coverage                | %                 | ≥ 70%       | TBD        | Based on the coverage of existing automated tests                                       |

## 

## 

## 6. Risks and Mitigations 

| **\#** | **Risk**                                                                                                                                           | **Prob** | **Impact** | **Mitigation**                                                                                                        |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------|-----------------------------------------------------------------------------------------------------------------------|
| R01    | System requirements may change after the design baseline has been established.                                                                     | Medium   | High       | Finalize the baseline after Week 5. Evaluate the impact of every change before accepting it.                          |
| R02    | Japanese language knowledge data entered into the system may be incomplete, inconsistent, duplicated, or incorrect.                                | High     | High       | Establish rules for checking and validating data before using it in the system.                                       |
| R03    | Dependencies between Knowledge Data, Courses, Learning Paths, Assessments, Learning Progress, and AI Recommendations may cause integration errors. | Medium   | High       | Identify data relationships and API agreements early. Perform continuous integration testing during development.      |
| R04    | AI-generated recommendations, practice exercises, or mock exams may be unsuitable or inaccurate.                                                   | Medium   | Medium     | Limit AI based on Knowledge Data and predefined templates. Check and review AI-generated results before official use. |
| R05    | The team's capacity may be insufficient due to unexpected defects, integration issues, or the need to redo work.                                   | Medium   | High       | Prioritize MUST-HAVE features and reserve some resources/time for unplanned work.                                     |

# II. Management Approach

## 1. Project Process/Methodology

<img src="Report-2_Project_Management_Plan_media/media/image3.png" style="width:6.28438in;height:3.55556in" />

The JLPT Exam Preparation System project uses the Iterative and Incremental Development method. Instead of building the entire system at once, the project is divided into several iterations. Each iteration produces a functional part of the system, and these parts are gradually integrated into the final complete version.

In each iteration, the team performs the following steps: defining requirements, analysis and design, implementation (coding), testing, and evaluation. After each iteration, completed features are tested and evaluated to identify errors, collect feedback, and make adjustments for the next iteration. This approach helps manage progress more effectively, identify problems early, and reduce risks during the integration of the entire system.

The project includes three main iterations:

- Iteration 1: Build the system foundation, account management, and JLPT proficiency assessment.

- Iteration 2: Develop learning paths, learning content, courses, flashcards, , and progress tracking features.

- Iteration 3: Complete JLPT mock exams, question management, reports, and administration features.

Project progress is managed using ProjectLibre and GitHub; documents are stored on Google Drive; diagrams are designed using DrawIO; and the completed system is deployed on Railway.

## 2. Quality Management

The quality management process is applied throughout development to ensure that the system meets the requirements in the SRS, operates reliably, and minimizes errors.

Defect Prevention: Before development begins, team members review the SRS document, Use Cases, system design, database diagrams, and related requirements. Any unclear requirements are discussed and agreed upon before coding begins.

Review Process: Documents and source code go through an internal review process before being used or merged. Source code is managed on GitHub, with reviews carried out through Pull Requests.

Coding Standards: The Backend uses Java/Spring Boot, while the Frontend uses Thymeleaf. The team follows agreed naming rules and code structure and ensures that sensitive information is not exposed by hard-coding it directly into the source code.

Unit Testing: Important functions—such as login, scoring, quizzes, mock exams, and progress updates—are tested before integration.

Integration and System Testing: The team checks the connections between modules and the entire business process, including proficiency assessment, learning path creation, learning sessions, and JLPT mock exams.

Defect Management: Errors are recorded and tracked on GitHub. After an error is fixed, the related function is tested again before the issue is closed.

## 3. Project Training Plan

| **Training Area**                          | **Participants**                      | **When, Duration**                               |
|--------------------------------------------|---------------------------------------|--------------------------------------------------|
| <span class="mark">Java Spring Boot</span> | <span class="mark">All members</span> | <span class="mark">Weeks 1-5.</span> Self-study  |
| <span class="mark">Git, Github</span>      | <span class="mark">All members</span> | <span class="mark">Week 1.</span> Self-study     |
| Thymeleaf                                  | <span class="mark">All members</span> | <span class="mark">Weeks 1-4.</span> Self-study  |
| Software Testing (Unit, Integration)       | <span class="mark">Longbm</span>      | <span class="mark">Weeks 5.</span> Self-study    |
| Gemini AI API Integration                  | <span class="mark">All members</span> | <span class="mark">Weeks 3–5 · Self-study</span> |

## 4. Responsibility Assignments

<img src="Report-2_Project_Management_Plan_media/media/image2.png" style="width:6.28438in;height:3.55556in" />

> **RACI Chart**: R~Responsible, A~Accountable, C~Consulted, I~Informed

<table>
<colgroup>
<col style="width: 47%" />
<col style="width: 11%" />
<col style="width: 11%" />
<col style="width: 9%" />
<col style="width: 10%" />
<col style="width: 10%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Work Package</strong></th>
<th><blockquote>
<p><strong>PM</strong></p>
<p><strong>(HoangHH)</strong></p>
</blockquote></th>
<th><blockquote>
<p><strong>BL</strong></p>
<p><strong>(HoangNT)</strong></p>
</blockquote></th>
<th><blockquote>
<p><strong>DL</strong></p>
<p><strong>(HungNH)</strong></p>
</blockquote></th>
<th><blockquote>
<p><strong>TL</strong></p>
<p><strong>(LongBM)</strong></p>
</blockquote></th>
<th><blockquote>
<p><strong>FL</strong></p>
<p><strong>(MinhPD)</strong></p>
</blockquote></th>
</tr>
<tr class="odd">
<th>Report-1_Project Introduction</th>
<th>A</th>
<th>C</th>
<th>R</th>
<th>C</th>
<th>C</th>
</tr>
<tr class="header">
<th>Report-2_Project Management Plan</th>
<th>A</th>
<th>C</th>
<th>R</th>
<th>C</th>
<th>C</th>
</tr>
<tr class="odd">
<th>Report-2_Project Tracking</th>
<th>A</th>
<th>C</th>
<th>R</th>
<th>C</th>
<th>C</th>
</tr>
<tr class="header">
<th>Report-2_Project Weekly Report</th>
<th>A</th>
<th>C</th>
<th>R</th>
<th>C</th>
<th>C</th>
</tr>
<tr class="odd">
<th>Report-3_Software Requirement Specification (incl. PRD, UCS, FDS)</th>
<th>A</th>
<th>R</th>
<th>R</th>
<th>C</th>
<th>R</th>
</tr>
<tr class="header">
<th>Report-4_Software Design Specification (incl. TDS)</th>
<th>A</th>
<th>R</th>
<th>R</th>
<th>C</th>
<th>R</th>
</tr>
<tr class="odd">
<th>Report-5.0_Test Documentation (Test Planning)</th>
<th>C</th>
<th>C</th>
<th>R</th>
<th>A/R</th>
<th>C</th>
</tr>
<tr class="header">
<th>Feature code Back-End (Iter 1-3)</th>
<th>I/C</th>
<th>A/R</th>
<th>I</th>
<th>I</th>
<th>I</th>
</tr>
<tr class="odd">
<th>Feature code Front-End (Iter 1-3)</th>
<th>I/C</th>
<th>I</th>
<th>I</th>
<th>I</th>
<th>A/R</th>
</tr>
<tr class="header">
<th>Report-5.1_Unit Test</th>
<th>A</th>
<th>C</th>
<th>C</th>
<th>R</th>
<th>C</th>
</tr>
<tr class="odd">
<th>Report-5.2_Integration Test</th>
<th>A</th>
<th>C</th>
<th>C</th>
<th>R</th>
<th>C</th>
</tr>
<tr class="header">
<th>Report-5.3_System Test_FRs</th>
<th>A</th>
<th>C</th>
<th>C</th>
<th>R</th>
<th>C</th>
</tr>
<tr class="odd">
<th>Report-5.4_System Test_NFRs</th>
<th>A</th>
<th>C</th>
<th>C</th>
<th>R</th>
<th>C</th>
</tr>
<tr class="header">
<th>Report-5.5_Acceptance Test Scripts (incl. E2E)</th>
<th>A</th>
<th>C</th>
<th>C</th>
<th>R</th>
<th>C</th>
</tr>
<tr class="odd">
<th>Report-6_Software User Guides (incl. Deployment Guide)</th>
<th>A/R</th>
<th>I</th>
<th>I</th>
<th>I</th>
<th>R</th>
</tr>
<tr class="header">
<th>Report-7_Final Project Document</th>
<th>A/R</th>
<th>C</th>
<th>I</th>
<th>C</th>
<th>R</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 5. Project Communications

<table>
<colgroup>
<col style="width: 26%" />
<col style="width: 16%" />
<col style="width: 27%" />
<col style="width: 17%" />
<col style="width: 11%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Communication Item</strong></th>
<th><strong>Who/ Target</strong></th>
<th><strong>Purpose</strong></th>
<th><strong>When, Fre quency</strong></th>
<th><strong>Type, Tool, Method(s)</strong></th>
</tr>
<tr class="odd">
<th>Daily Meeting</th>
<th>All members</th>
<th>Plan the next day's tasks, check progress, and identify blockers</th>
<th>Every day at 9:00 PM</th>
<th>Google Meet</th>
</tr>
<tr class="header">
<th>Weekly Supervisor Meeting</th>
<th>All team members Supervisor</th>
<th>Update progress, receive feedback, approve deliverables, and report issues that need to be addressed</th>
<th>Every Friday at 9:00 PM</th>
<th>Google Meet</th>
</tr>
<tr class="odd">
<th>Ad-hoc Meeting</th>
<th><blockquote>
<p>All team members</p>
</blockquote></th>
<th>Resolve urgent issues outside scheduled meetings</th>
<th>When necessary</th>
<th>Google Meet</th>
</tr>
<tr class="header">
<th>Team Zalo Channel</th>
<th>All team members</th>
<th>Daily communication, decision-making, and announcements. Expected response time: <strong>10 minutes – 2 hours</strong></th>
<th>Continuously</th>
<th>Internal Zalo group</th>
</tr>
<tr class="odd">
<th>Supervisor Communication Channel</th>
<th>All team members + Supervisor</th>
<th>Communicate with the supervisor outside scheduled meetings. Expected response time: <strong>30 minutes – 2 hours</strong></th>
<th>Continuously</th>
<th>Zalo</th>
</tr>
<tr class="header">
<th>Task Tracking</th>
<th>All team members</th>
<th>Track work progress and task status by stage.</th>
<th><blockquote>
<p>Continuously</p>
</blockquote></th>
<th>Google Sheets</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 6. Configuration Management

### 6.1 Document Management

Project documents—such as the SRS, Project Plan, Use Cases, diagrams, and other related documents—are stored in one shared location on the team's Google Drive.

Documents are organized into folders based on specific project stages or document types. File naming rules must be consistent to make searching and management easier.

Documents are updated whenever important changes occur.

Major changes must be reviewed by the team before being included in the official version. Once approved, documents must not be edited without notifying the relevant team members.

Google Drive is also used to track editing history and allow previous versions to be restored in case of errors or unsuitable changes.

### 6.2 Source Code Management

The project's source code is managed using Git and stored on GitHub to support version control and teamwork.

The main source code repository is organized into the following branches:

- main: Contains the stable version of the source code.

- develop: Contains source code currently being integrated and developed.

- feature/...: Used for specific features.

- bugfix/...: Used for fixing system errors.

### 6.3 Tools & Infrastructures

| **Category**           | **Tools / Infrastructure**                      |
|------------------------|-------------------------------------------------|
| **Technology**         | Thymeleaf (FrontEnd), Java/SpringBoot (BackEnd) |
| **Database**           | PostgreSQL, SupaBase                            |
| **IDEs/Editors**       | Visual Studio Code                              |
| **Diagramming**        | DrawIO                                          |
| **Documentation**      | Google Docs/Sheets/Slides                       |
| **Version Control**    | GitHub (Source Codes), Google Drive (Documents) |
| **Deployment server**  | Railway (Frontend + Backend + DB)               |
| **Project management** | ProjectLibre (Schedule), GitHub                 |
