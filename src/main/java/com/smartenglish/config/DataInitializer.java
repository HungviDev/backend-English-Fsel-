package com.smartenglish.config;

import com.smartenglish.entity.*;
import com.smartenglish.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final CourseRepository courseRepository;
        private final LessonRepository lessonRepository;
        private final LessonExerciseRepository exerciseRepository;
        private final PlacementTestRepository placementTestRepository;
        private final QuestionPassageRepository passageRepository;
        private final PlacementQuestionRepository questionRepository;
        private final UserTestAttemptRepository attemptRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) {
                log.info("Checking initial system database seeds...");

                // 1. Seed Roles (ROLE_ADMIN, ROLE_STUDENT)
                Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                                .orElseGet(() -> roleRepository.save(Role.builder()
                                                .roleName("ROLE_ADMIN")
                                                .description("Quản trị viên toàn quyền hệ thống")
                                                .build()));

                Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                                .orElseGet(() -> roleRepository.save(Role.builder()
                                                .roleName("ROLE_STUDENT")
                                                .description("Học viên học tiếng Anh")
                                                .build()));

                // 2. Seed Default Admin User
                User admin = userRepository.findByEmail("admin@smartenglish.com")
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .email("admin@smartenglish.com")
                                                .passwordHash(passwordEncoder.encode("Admin@123456"))
                                                .fullName("System Administrator")
                                                .role(adminRole)
                                                .isActive(true)
                                                .isPlacementCompleted(true)
                                                .currentLevel("C2")
                                                .build()));

                // 3. Seed Default Student User (Ban đầu chưa làm test để kiểm chứng Giai đoạn
                // 2)
                User student = userRepository.findByEmail("student@smartenglish.com")
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .email("student@smartenglish.com")
                                                .passwordHash(passwordEncoder.encode("Student@123456"))
                                                .fullName("Nguyễn Văn Học")
                                                .role(studentRole)
                                                .isActive(true)
                                                .isPlacementCompleted(false)
                                                .currentLevel("B1")
                                                .build()));

                // 4. Seed Course & Admin Lessons for Menu 2 & RAG Knowledge Catalog
                if (courseRepository.count() == 0) {
                        Course b1Course = courseRepository.save(Course.builder()
                                        .title("Khóa học B1 Intermediate English Mastery")
                                        .description("Khóa học bồi dưỡng nền tảng toàn diện 4 kỹ năng chuẩn CEFR B1 cho học viên.")
                                        .targetLevel("B1")
                                        .orderIndex(1)
                                        .isPublished(true)
                                        .createdBy(admin)
                                        .build());

                        Lesson l1 = lessonRepository.save(Lesson.builder()
                                        .course(b1Course)
                                        .title("Mastering Listening Details in Everyday Conversations")
                                        .skillType("LISTENING")
                                        .subSkillTag("LISTENING_DETAIL")
                                        .contentHtml("<p>Chiến thuật bắt từ khóa và lọc nhiễu âm trong hội thoại ngắn tốc độ 140 wpm.</p>")
                                        .estimatedMinutes(15)
                                        .orderIndex(1)
                                        .isPublished(true)
                                        .build());

                        Lesson l2 = lessonRepository.save(Lesson.builder()
                                        .course(b1Course)
                                        .title("Essential Academic Vocabulary & Collocations")
                                        .skillType("READING")
                                        .subSkillTag("READING_VOCAB")
                                        .contentHtml("<p>Phương pháp đoán nghĩa từ học thuật qua gốc từ (Roots, Prefixes, Suffixes).</p>")
                                        .estimatedMinutes(20)
                                        .orderIndex(2)
                                        .isPublished(true)
                                        .build());

                        Lesson l3 = lessonRepository.save(Lesson.builder()
                                        .course(b1Course)
                                        .title("Grammar in Action: Present Perfect vs Past Simple")
                                        .skillType("GRAMMAR")
                                        .subSkillTag("GRAMMAR_TENSE")
                                        .contentHtml("<p>Phân biệt thì hiện tại hoàn thành và quá khứ đơn trong bài đọc thực tế.</p>")
                                        .estimatedMinutes(15)
                                        .orderIndex(3)
                                        .isPublished(true)
                                        .build());

                        // Thêm Lesson Exercises cho Lesson 1 & 2
                        exerciseRepository.save(LessonExercise.builder()
                                        .lesson(l1)
                                        .questionText("In conversation listening, what are content words?")
                                        .optionA("Prepositions and articles")
                                        .optionB("Nouns, main verbs, and adjectives")
                                        .optionC("Conjunctions only")
                                        .optionD("Auxiliary verbs")
                                        .correctOption("B")
                                        .explanation("Content words carry meaning and include nouns, verbs, adjectives, and adverbs.")
                                        .build());

                        exerciseRepository.save(LessonExercise.builder()
                                        .lesson(l2)
                                        .questionText("Which word forms a common academic collocation with 'sustainable'?")
                                        .optionA("development")
                                        .optionB("eating")
                                        .optionC("jumping")
                                        .optionD("sleeping")
                                        .correctOption("A")
                                        .explanation("'Sustainable development' is one of the most frequent collocations in academic English.")
                                        .build());

                        log.info("Seeded initial Course, Lessons, and Exercises.");
                }

                // 5. Seed Placement Test with Passages and Questions (Giai đoạn 1 Data
                // Foundation)
                if (placementTestRepository.count() == 0) {
                        PlacementTest placementTest = placementTestRepository.save(PlacementTest.builder()
                                        .title("Bài kiểm tra đánh giá năng lực CEFR Đầu vào (Nghe & Đọc)")
                                        .description("Bài test chuẩn đoán năng lực phân loại từ A1 đến C1 gồm 2 phần Nghe và Đọc.")
                                        .durationMinutes(45)
                                        .totalQuestions(4)
                                        .isActive(true)
                                        .createdBy(admin)
                                        .build());

                        // Passage 1: Listening
                        QuestionPassage listeningPassage = passageRepository.save(QuestionPassage.builder()
                                        .test(placementTest)
                                        .skillType("LISTENING")
                                        .title("Part 1: Daily Workplace Conversation")
                                        .passageText("Audio transcript: Speaker A invites Speaker B to the upcoming international project conference...")
                                        .audioUrl("https://assets.smartenglish.com/audio/sample_placement_l1.mp3")
                                        .orderIndex(1)
                                        .build());

                        questionRepository.save(PlacementQuestion.builder()
                                        .passage(listeningPassage)
                                        .subSkillTag("LISTENING_DETAIL")
                                        .difficultyLevel("B1")
                                        .questionText("What time is the international conference scheduled to begin?")
                                        .optionA("8:30 AM")
                                        .optionB("9:30 AM")
                                        .optionC("10:00 AM")
                                        .optionD("2:00 PM")
                                        .correctOption("B")
                                        .explanation("The speaker clearly confirmed: 'The conference will start at 9:30 AM sharp.'")
                                        .build());

                        questionRepository.save(PlacementQuestion.builder()
                                        .passage(listeningPassage)
                                        .subSkillTag("LISTENING_DETAIL")
                                        .difficultyLevel("B1")
                                        .questionText("Where will the attendees receive their name badges?")
                                        .optionA("At the main entrance reception desk")
                                        .optionB("In the hotel restaurant")
                                        .optionC("At the airport terminal")
                                        .optionD("In the basement parking")
                                        .correctOption("A")
                                        .explanation("Badges are collected at the main entrance reception desk.")
                                        .build());

                        // Passage 2: Reading
                        QuestionPassage readingPassage = passageRepository.save(QuestionPassage.builder()
                                        .test(placementTest)
                                        .skillType("READING")
                                        .title("Part 2: Environmental Science Article")
                                        .passageText("Biodiversity is essential for maintaining resilient natural ecosystems. However, recent scientific surveys indicate that rapid urbanization has led to unprecedented fragmentation of natural habitats...")
                                        .audioUrl(null)
                                        .orderIndex(2)
                                        .build());

                        questionRepository.save(PlacementQuestion.builder()
                                        .passage(readingPassage)
                                        .subSkillTag("READING_VOCAB")
                                        .difficultyLevel("B1")
                                        .questionText("In the passage, the word 'fragmentation' is closest in meaning to:")
                                        .optionA("Division into smaller separated parts")
                                        .optionB("Complete destruction and disappearance")
                                        .optionC("Rapid expansion")
                                        .optionD("Natural preservation")
                                        .correctOption("A")
                                        .explanation("'Fragmentation' means the process of breaking into distinct fragments or pieces.")
                                        .build());

                        questionRepository.save(PlacementQuestion.builder()
                                        .passage(readingPassage)
                                        .subSkillTag("READING_VOCAB")
                                        .difficultyLevel("B1")
                                        .questionText("The term 'resilient' implies that ecosystems are:")
                                        .optionA("Very weak and easily destroyed")
                                        .optionB("Able to withstand and recover quickly from shocks")
                                        .optionC("Completely unchanging over centuries")
                                        .optionD("Uninhabited by humans")
                                        .correctOption("B")
                                        .explanation("'Resilient' describes the capacity to recover quickly from difficulties.")
                                        .build());

                        log.info("Seeded initial Placement Test with Passages and Questions.");
                }

                log.info("Database Initialization completed successfully!");
        }
}
