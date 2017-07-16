package tcd.edu.skillextractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import tcd.edu.skillextractor.bean.JDRequirementList;
import tcd.edu.skillextractor.bean.Requirement;
import tcd.edu.skillextractor.bean.Skill;

public class JobDescriptionParser {

	public List<Requirement> parseJD(List<String> jobDesc) {
		SkillLoader skillLoader = new SkillLoader();

		List<Requirement> availSkills = skillLoader.loadAllRequirements(Constant.skillFile);

		SkillFinder skillFinder = new SkillFinder();
		List<Requirement> requirements = skillFinder.findSkills(availSkills, jobDesc);

		skillFinder.assignLineScores(requirements, jobDesc);

		List<String> newReqList = skillLoader.removeStopChars(jobDesc);

		MaxentTagger tagger = new MaxentTagger("src/main/resources/taggers/left3words-wsj-0-18.tagger");

		List<JDRequirementList> jdReqList = tagAndFetchJDRequirements(tagger, newReqList);

		QuantifiableExperienceExtractor expExtractor = new QuantifiableExperienceExtractor();
		expExtractor.scoreQuantifiableExperienceSkill(jdReqList, requirements);

		KeywordBasedScore keywordBasedScore = new KeywordBasedScore();
		keywordBasedScore.assignScoreBasedOnKeyword(jdReqList, requirements);

		for (Requirement requirement : requirements) {
			for (Skill skill : requirement.getSkills()) {
				double lineScore = skill.getLineScore() == null ? 0.0 : skill.getLineScore();
				double preferenceScore = skill.getPreferenceScore() == null ? 0.0 : skill.getPreferenceScore();
				skill.setFinalScore(((lineScore + preferenceScore) * 10) / 8);
			}
		}

		return requirements;
	}

	private static List<JDRequirementList> tagAndFetchJDRequirements(MaxentTagger tagger, List<String> newReqList) {
		List<JDRequirementList> jdRequirementList = new ArrayList<JDRequirementList>();
		int i = 0;
		for (String req : newReqList) {
			JDRequirementList jdReq = new JDRequirementList();
			jdReq.setRequirementString(req);
			jdReq.setTaggedRequirementString(tagger.tagString(replaceWordWithInt(req)).toLowerCase().trim());
			jdReq.setLineIndex(i);
			jdRequirementList.add(jdReq);
			i++;
		}

		return jdRequirementList;
	}

	private static String replaceWordWithInt(String req) {
		String[] words = { "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten" };
		req = req.toLowerCase();
		for (int i = 0; i < words.length; i++) {
			if (patternMatches(req, words[i])) {
				req = req.replaceAll("\\b(" + words[i] + ")\\b", String.valueOf(i + 1));
			}
		}
		return req;
	}

	private static boolean patternMatches(String reqString, String Skill) {
		String regex = "\\b(" + Skill + ")\\b";
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(reqString);
		return matcher.find();
	}
}
