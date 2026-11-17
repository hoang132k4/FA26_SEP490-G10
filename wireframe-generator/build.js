const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { createTar } = require('./common');

const {
  buildRecommendedCourses,
  buildMyCourses,
  buildCourseRegistration,
  buildLessonStudy
} = require('./pages_part1');

const {
  buildFlashcardList,
  buildFlashcardDetail,
  buildCreateFlashcard,
  buildFlipCardPractice
} = require('./pages_part2');

const {
  buildMultipleChoiceTest,
  buildVocabularyWordMatch,
  buildQuizTaking,
  buildQuizResults
} = require('./pages_part3');

const {
  buildTakeExam
} = require('./pages_exam');

const pageDefinitions = [
  { id: 'a1b2c3d4e5f647a8b9c0d1e2f3a4b5c6', name: 'Recommended Courses', builder: buildRecommendedCourses },
  { id: 'b2c3d4e5f6a748b9c0d1e2f3a4b5c6d7', name: 'My Courses', builder: buildMyCourses },
  { id: 'c3d4e5f6a7b849c0d1e2f3a4b5c6d7e8', name: 'Course Registration', builder: buildCourseRegistration },
  { id: 'd4e5f6a7b8c950d1e2f3a4b5c6d7e8f9', name: 'Lesson Study', builder: buildLessonStudy },
  { id: 'e5f6a7b8c9d051e2f3a4b5c6d7e8f9a0', name: 'Flashcard List', builder: buildFlashcardList },
  { id: 'f6a7b8c9d0e152f3a4b5c6d7e8f9a0b1', name: 'Flashcard Detail', builder: buildFlashcardDetail },
  { id: 'a7b8c9d0e1f253a4b5c6d7e8f9a0b1c2', name: 'Create Flashcard', builder: buildCreateFlashcard },
  { id: 'b8c9d0e1f2a354b5c6d7e8f9a0b1c2d3', name: 'Flashcard Practice (Flip Card)', builder: buildFlipCardPractice },
  { id: 'c9d0e1f2a3b455c6d7e8f9a0b1c2d3e4', name: 'Flashcard Practice (Multiple Choice Test)', builder: buildMultipleChoiceTest },
  { id: 'd0e1f2a3b4c556d7e8f9a0b1c2d3e4f5', name: 'Vocabulary Word Match', builder: buildVocabularyWordMatch },
  { id: 'e1f2a3b4c5d657e8f9a0b1c2d3e4f5a6', name: 'Quiz Taking', builder: buildQuizTaking },
  { id: 'f2a3b4c5d6e758f9a0b1c2d3e4f5a6b7', name: 'Quiz Results', builder: buildQuizResults },
  { id: 'a0b1c2d3e4f546a7b8c9d0e1f2a3b4c5', name: 'Phòng thi ảo (Take Exam)', builder: buildTakeExam }
];

function generatePageXml(id, name, contentXml) {
  return `<?xml version="1.0" encoding="UTF-8"?><p:Page xmlns:p="http://www.evolus.vn/Namespace/Pencil"><p:Properties><p:Property name="id">${id}</p:Property><p:Property name="name">${name}</p:Property><p:Property name="width">1440</p:Property><p:Property name="height">900</p:Property><p:Property name="pageFileName">page_${id}.xml</p:Property><p:Property name="zoom">1</p:Property></p:Properties><p:Content>${contentXml}</p:Content></p:Page>`;
}

function generateContentXml(pages, activeId) {
  const pageTags = pages.map(p => `<Page href="page_${p.id}.xml"/>`).join('');
  return `<?xml version='1.0' encoding='UTF-8'?><Document xmlns="http://www.evolus.vn/Namespace/Pencil"><Properties><Property name="activeId">${activeId}</Property></Properties><Pages>${pageTags}</Pages></Document>`;
}

function main() {
  console.log(`Generating ${pageDefinitions.length} Learner screens...`);
  const files = [];

  pageDefinitions.forEach((p, idx) => {
    console.log(`[${idx + 1}/${pageDefinitions.length}] Building ${p.name}...`);
    const content = p.builder();
    const xml = generatePageXml(p.id, p.name, content);
    files.push({
      name: `page_${p.id}.xml`,
      content: xml
    });
  });

  // Set activeId to Screen 13 (Take Exam) so Pencil opens directly to it
  const examPageId = 'a0b1c2d3e4f546a7b8c9d0e1f2a3b4c5';
  const contentXml = generateContentXml(pageDefinitions, examPageId);
  files.push({
    name: 'content.xml',
    content: contentXml
  });

  console.log('Creating tar archive...');
  const tarBuffer = createTar(files);
  console.log('Tar size:', tarBuffer.length, 'bytes');

  console.log('GZipping into .epgz...');
  const epgzBuffer = zlib.gzipSync(tarBuffer, { level: 9 });
  console.log('EPGZ size:', epgzBuffer.length, 'bytes');

  const outPath = path.resolve(__dirname, '..', 'JEPS-Learner-Course-Screens.epgz');
  fs.writeFileSync(outPath, epgzBuffer);
  console.log('Saved successfully to:', outPath);

  // Also export standalone single-screen file for easy direct viewing
  const standaloneExamDef = [pageDefinitions.find(p => p.id === examPageId)];
  const standaloneContent = standaloneExamDef[0].builder();
  const standaloneXml = generatePageXml(examPageId, standaloneExamDef[0].name, standaloneContent);
  const standaloneContentXml = generateContentXml(standaloneExamDef, examPageId);
  const standaloneFiles = [
    { name: `page_${examPageId}.xml`, content: standaloneXml },
    { name: 'content.xml', content: standaloneContentXml }
  ];
  const standaloneTar = createTar(standaloneFiles);
  const standaloneEpgz = zlib.gzipSync(standaloneTar, { level: 9 });
  const standaloneOutPath = path.resolve(__dirname, '..', 'JEPS-Take-Exam-Phòng-Thi-Ảo.epgz');
  fs.writeFileSync(standaloneOutPath, standaloneEpgz);
  console.log('Standalone file saved to:', standaloneOutPath);
}

main();

