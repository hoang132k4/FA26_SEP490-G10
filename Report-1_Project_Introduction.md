<img src="Report-1_Project_Introduction_media/media/image1.png" style="width:2.961in;height:0.90999in" />

**<span class="smallcaps">CAPSTONE PROJECT REPORT</span>**

**Report 1 – Project Introduction**

> – Hanoi, Aug 2026 –

**Table of Contents**

[I. Record of Changes 3](#_heading=h.w2ynv0xfhdrt)

[II. Definition and Acronyms 4](#definition-and-acronyms)

[III. Project Introduction 4](#_heading=h.jlmk37d7uaj)

> [1. Overview 4](#i.-project-overview)
>
> [1.1 Project Information 4](#project-information)
>
> [1.2 Project Purpose 4](#project-purpose)
>
> [1.3 Project Stakeholders 4](#project-stakeholders)
>
> [2. Product Background 4](#ii.-product-background)
>
> [3. Existing Solutions 5](#iii.-existing-solutions)
>
> [3.1 \[Solution Name 1\] 5](#riki-nihongo)
>
> [3.2 Example - Workable (Enterprise ATS) 5](#_heading=h.hl7nkmhhe5t)
>
> [3.3 \[Solution Name 3\] 6](#dung-mori)
>
> [3.4 Example - Google Sheets + Google Forms 6](#_heading=h.8d6fn7mve25v)
>
> [3.5 Section Summary 6](#_heading=h.a7lv2zlrykib)
>
> [4. Proposed Solution 6](#iv.-proposed-solution)
>
> [5. Project Scope & Limitations 7](#v.-project-scope-limitations)
>
> [5.1 Major Features 7](#major-features)
>
> [5.2 Limitations & Exclusions 8](#limitations-exclusions)

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
<th>11/9/2026</th>
<th>A</th>
<th>Hoanghh</th>
<th>Initial Creation</th>
</tr>
<tr class="header">
<th>12/9/2026</th>
<th>A</th>
<th>Hoanghh</th>
<th>Project Overview : 1.Project Information,2.Project Purpose</th>
</tr>
<tr class="odd">
<th>12/9/2026</th>
<th>A</th>
<th>Hoanghh</th>
<th>Product Background</th>
</tr>
<tr class="header">
<th>12/9/2026</th>
<th>A</th>
<th>Hoanghh</th>
<th>Existing Solutions</th>
</tr>
<tr class="odd">
<th>12/9/2026</th>
<th>A</th>
<th>Hoanghh</th>
<th>Proposed Solution</th>
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

*\[Fill all the definitions, acronyms,… used within the whole project in the table format as below\]*

| **Acronym** | **Definition**                      |
|-------------|-------------------------------------|
| JEPS        | JLPT Exam Preparation System        |
| JLPT        |  Japanese-Language Proficiency Test |
| API         | Application Programming Interface   |
| DB          | Database                            |
| RBAC        | Role-Based Access Control           |
| CRUD        | Create, Read, Update, Delete        |

# I. Project Overview

## 1. Project Information

- English name: ***JLPT Exam Preparation System***

- Vietnamese name: Hệ Thống Luyện Thi JLPT

- Project code: ***JEPS***

- Group name: ***SEP490-G10***

- Software type: ***Web Application***

## 2. Project Purpose

The objective of the JLPT Exam Preparation System is to help learners prepare for JLPT exams from N5 to N1 through a personalized learning approach and by providing courses from N5 to N1. The system assesses learners' current proficiency levels, creates suitable learning paths, recommends courses, tracks learning progress, and provides mock exams. Based on the results, AI evaluates and provides learning paths that can be adjusted to better meet each learner's needs.

## 3. Project Stakeholders

| **Full Name**     | **Role** | **Email**                  | **Mobile** |
|-------------------|----------|----------------------------|------------|
| Nguyen Trung Kien | Lecturer | kiennt@fe.edu.vn           | 0912345678 |
| Huynh Huy Hoang   | Leader   | hoanghhhe180462@fpt.edu.vn | 0979431974 |
| Phi Duc Minh      | Member   | minhpdhe181866@fpt.edu.vn  | 0385372991 |
| Nguyen Tuan Hoang | Member   | hoangnthe180946@fpt.edu.vn | 0325285353 |
| Bui Minh Long     | Member   | longbmhe182468@fpt.edu.vn  | 0386945619 |
| Nguyen Hoang Hung | Member   | hungnhhe186547@fpt.edu.vn  | 0978856362 |

# 

# II. Product Background

The JLPT is a Japanese language proficiency test with levels ranging from N5 to N1. Currently, many platforms support JLPT learning and exam preparation, but most still use fixed learning paths for each level and do not provide detailed personalization based on each learner's results.

JEPS is developed to support personalized JLPT self-study and provide courses from N5 to N1. The system focuses on proficiency assessment, using AI to identify strengths and weaknesses, creating suitable learning paths, providing learning content, practice activities, mock exams, and tracking progress from N5 to N1.

# III. Existing Solutions

## 1. Riki Nihongo 

<table>
<colgroup>
<col style="width: 20%" />
<col style="width: 79%" />
</colgroup>
<thead>
<tr class="header">
<th><em><strong>Website / Link</strong></em></th>
<th><a href="https://riki.edu.vn/"><u>Riki Nihongo - Học Riki Thi Là Đỗ JLPT</u></a></th>
</tr>
<tr class="odd">
<th><em><strong>Target Users</strong></em></th>
<th>Beginners learning Japanese; learners studying from N5–N1; people preparing for the JLPT; international students, interns, and working adults.</th>
</tr>
<tr class="header">
<th><em><strong>Core Features</strong></em></th>
<th>JLPT courses by level; video lessons; exercises with instant grading; vocabulary flashcards; learning material library; listening, reading, vocabulary, Kanji, and grammar practice; teacher guidance and learning paths; support for learning on multiple devices.</th>
</tr>
<tr class="odd">
<th><em><strong>Strengths</strong></em></th>
<th>Clear JLPT learning paths, regular tests, teacher support, various learning materials, and personalized learning guidance.</th>
</tr>
<tr class="header">
<th><em><strong>Weaknesses</strong></em></th>
<th><p>- No AI learning support.</p>
<p>- No detailed learning path for each learner.</p>
<p>- No automatic learning path updates after each assessment based on individual learner results.</p>
<p>- No payment system.</p></th>
</tr>
<tr class="odd">
<th><em><strong>GAP for this project</strong></em></th>
<th><p>- Provide AI learning support.</p>
<p>- Provide detailed learning paths for each learner.</p>
<p>- Automatically update learning paths after each assessment based on individual learner results.</p>
<p>- AI provides detailed evaluations after each JLPT mock exam and test.</p>
<p>- Di<strong>rect payment on the system.</strong></p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 

## 

## 2. Dung Mori

<table>
<colgroup>
<col style="width: 20%" />
<col style="width: 79%" />
</colgroup>
<thead>
<tr class="header">
<th><em><strong>Website / Link</strong></em></th>
<th>https://dungmori.com/</th>
</tr>
<tr class="odd">
<th><em><strong>Target Users</strong></em></th>
<th>Vietnamese learners of Japanese, especially students, working adults, beginners, and self-learners who want to study Japanese and prepare for the JLPT from N5 to N1.</th>
</tr>
<tr class="header">
<th><em><strong>Core Features</strong></em></th>
<th><p>JLPT courses from N5–N1.</p>
<p>• Video lessons on Web/App, online learning through Zoom, and in-person learning at the center.</p>
<p>• Vocabulary, Kanji, grammar, reading comprehension, and listening comprehension.</p>
<p>• Vocabulary flashcards with audio.</p>
<p>• Learning paths divided by day/stage.</p>
<p>• JLPT tests and practice exams.</p>
<p>• Online mock exams with a structure similar to the JLPT and immediate results.</p>
<p>• Teacher support to answer questions.</p></th>
</tr>
<tr class="odd">
<th><em><strong>Strengths</strong></em></th>
<th><p>Content covers all levels from N5–N1.</p>
<p>• An online learning system combining App/Web/Zoom.</p>
<p>• Learning paths are designed quite clearly for each level and stage.</p>
<p>• Many lessons, tests, and practice exams.</p>
<p>• Supports both self-study and teacher-guided learning.</p>
<p>• Mock exams help learners assess their own abilities.</p></th>
</tr>
<tr class="header">
<th><em><strong>Weaknesses</strong></em></th>
<th><p>• Although it offers "personalized learning paths," publicly available information mainly describes pre-designed learning paths by level/day. There is not yet a comprehensive initial assessment system that automatically adjusts learning paths based on each learner's results.</p>
<p>• Courses and practice exercises follow a pre-designed structure, so adaptive/personalized learning may still be limited.</p>
<p>• Learners still have to choose suitable courses/learning paths based on their levels and goals.</p></th>
</tr>
<tr class="odd">
<th><em><strong>GAP for this project</strong></em></th>
<th>Build a JLPT Preparation system with clearer personalization and adaptive learning: assess initial proficiency → identify strengths/weaknesses → automatically create a suitable learning path → recommend courses/materials → track progress → organize mock exams → update learning paths based on learning and mock exam results.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## 

# IV. Proposed Solution

JEPS proposes an online JLPT exam preparation platform that brings learning features together in one unified system.

The system allows learners to take an initial assessment to determine their proficiency level. AI analyzes the results, identifies strengths, weaknesses, and knowledge gaps, then recommends courses so users can enroll in the most suitable courses and helps create appropriate learning paths. JEPS provides a Japanese language knowledge database including Vocabulary, Kanji, Kana, Grammar, listening comprehension, and reading comprehension, combined with JLPT mock exams, flashcards, quizzes, courses, and practice exercises.

During the learning process, the system records results, learning history, and incorrect answers. AI continues to analyze learning performance, recommends content that needs to be reviewed, and helps update learning paths based on learner results.

General system flow:

Assessment → AI Analysis → Course Recommendation → Learning Path → Learning → Practice/Quiz → Mock Exam → AI Progress & Weakness Analysis → AI Review Recommendation → Update Learning Path → Continue Learning.

# V. Project Scope & Limitations

## 1. Major Features

| **ID**    | **Feature Name**                                           | **Description**                                                                                                                                                                                                                                      | **Addressed Gaps**     |
|-----------|------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| FE-01     | Japanese Proficiency Assessment and Learning Path Creation | The system provides an initial Japanese proficiency assessment to determine the learner's current level. Based on assessment results and learning goals, AI helps create a suitable learning path for each learner.                                  | GAP-01                 |
| FE-02     | Japanese Language Learning                                 | Provides structured Japanese learning content based on a centralized knowledge database, including Vocabulary, Grammar, Kanji, Kana, Listening Comprehension, and Reading Comprehension, organized by JLPT levels from N5 to N1.                     | GAP-01, GAP-02         |
| **FE-03** | **Providing Courses from N5 to N1**                        | The system provides Japanese courses for JLPT levels from N5 to N1, including lessons, Flashcards, practice exercises, and assessment tests, helping learners study in a structured way and track their progress.                                    | GAP-01, GAP-03         |
| FE-04     | Flashcard Learning                                         | Helps learners remember and review vocabulary through Flashcards, saved words, bookmarked content, and personal learning lists. This content is directly linked to the system's Japanese knowledge database.                                         | GAP-03                 |
| FE-05     | Practice Exercises and Quizzes                             | Provides practice activities and interactive Quizzes connected to Japanese learning content, allowing learners to practice Vocabulary, Grammar, Kanji, and related content after completing lessons.                                                 | GAP-03, GAP-04         |
| FE-06     | JLPT Mock Exam Practice                                    | Provides practice exercises and mock exams following the JLPT format, based on supported knowledge content and JLPT levels, helping learners evaluate their learning results and readiness for the exam.                                             | GAP-04                 |
| FE-07     | Progress Tracking and Strength and Weakness Analysis       | The system records learning history, completed activities, Quiz results, and exam results for AI to analyze strengths and weaknesses, helping learners clearly track their learning progress and identify areas where their knowledge is still weak. | GAP-01, GAP-04         |
| FE-08     | Personalized Review Content Recommendations                | Based on learners' results and incorrect answers, AI performs rule-based analysis to recommend suitable Vocabulary, Grammar, Kanji, lessons, or practice activities that need to be reviewed.                                                        | GAP-01, GAP-03, GAP-04 |

## 

## 2. Limitations & Exclusions

| **ID** | **Excluded Item**                                | **Reason**                                                                                                                                                                                                                                                                                       |
|--------|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| LI-01  | Teacher and Lecturer Management                  | The current version of JEPS mainly focuses on Japanese self-study. Therefore, the system does not yet support teacher accounts, lecturer management, or direct interaction between teachers and learners because JEPS is not currently designed as an online Japanese language center or school. |
| LI-02  | Online Classes and Class Registration Management | Features such as live online classes, class scheduling, attendance management, and class registration are not included in the current scope. JEPS focuses on supporting self-study rather than operating a complete online education system.                                                     |
| LI-04  | Speaking and Pronunciation Assessment            | The system does not yet support automatic pronunciation scoring or advanced speaking skill assessment. The current version mainly focuses on Japanese language learning, practice, JLPT exam preparation, and learning progress tracking.                                                        |
| LI-05  | Native Mobile Applications                       | JEPS is initially developed as a web application using ReactJS and Spring Boot. Developing separate Native applications for Android and iOS is outside the scope of the current version.                                                                                                         |
| LI-06  | Official JLPT Registration and Certification     | JEPS only provides learning materials, practice exercises, and JLPT-oriented mock exams. The system does not support registration for official JLPT exams, organizing official exams, or issuing JLPT certificates.                                                                              |
| LI-07  | Social Networking Features                       | Features such as public communities, user-following systems, direct messaging, or social news feeds are not implemented because they are not part of the platform's core learning process.                                                                                                       |
