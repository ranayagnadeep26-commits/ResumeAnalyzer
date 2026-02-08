package com.cfs.aiAnalyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin("*")
public class ResumeController {

    private final ChatClient chatClient;

    private final Tika tika = new Tika();

    public ResumeController(OpenAiChatModel openAiChatModel) {
        this.chatClient = ChatClient.create(openAiChatModel);
    }

    @PostMapping("/analyze")
    public Map<String, String> analyser(@RequestParam("file") MultipartFile file) throws Exception {

        String content = tika.parseToString(file.getInputStream());

        String prompt = """
Analyze the resume below and return the result STRICTLY in this format.
Do NOT write explanations.
Do NOT repeat instructions.

FORMAT:

SKILLS:
resume me kiya skills mantion he wo do 


RATING:
cheking by your side how many rating given to this resume

IMPROVEMENT:
apne apse resume dekh kar three improvement dedo 

Resume:
%s
""".formatted(content);

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return Map.of("result", aiResponse);
    }

    @PostMapping("/ats-check")
    public Map<String, Object> analyzeATS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jd") String jobDescription) throws Exception {

        String resumeText = tika.parseToString(file.getInputStream());

        String prompt = """
            Compare resume and job description and return ONLY valid JSON.

            FORMAT:
            {
              "atsScore": 75,
              "matchedKeywords": ["Java", "Spring Boot"],
              "missingKeywords": ["Docker", "AWS"],
              "summary": "Resume matches most backend requirements"
            }

            Resume:
            %s

            Job Description:
            %s
            """.formatted(resumeText, jobDescription);

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        ObjectMapper mapper = new ObjectMapper();

        // 🔒 clean ```json ``` if present
        String clean = aiResponse
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        return mapper.readValue(clean, Map.class);
    }
}