package tcd.edu.skillextractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import tcd.edu.skillextractor.bean.JDRequirementList;
import tcd.edu.skillextractor.bean.Requirement;
import tcd.edu.skillextractor.bean.Skill;

public class SkillExtractorMain {

	public static void main(String[] args) {
//		String[] files = { "posting1", "posting2", "posting3", "posting4", "posting5", "posting6", "posting7",
//				"posting8", "posting9", "posting10" };
		String[] files = {"posting10"};
		for (String filename : files) {
			BufferedReader br = null;
			List<String> reqList = new ArrayList<String>();
			try {

				br = new BufferedReader(new FileReader("src/main/resources/jobs/" + filename + ".txt"));
				String line = "";
				while ((line = br.readLine()) != null) {
					reqList.add(line.toLowerCase().trim());
				}
				br.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

			SkillLoader skillLoader = new SkillLoader();

			List<Requirement> availSkills = skillLoader.loadAllRequirements(Constant.skillFile);

			SkillFinder skillFinder = new SkillFinder();
			List<Requirement> foundSkills = skillFinder.findSkills(availSkills, reqList);

			skillFinder.assignLineScores(foundSkills, reqList);

			List<String> newReqList = skillLoader.removeStopChars(reqList);

			MaxentTagger tagger = new MaxentTagger("src/main/resources/taggers/left3words-wsj-0-18.tagger");

			List<JDRequirementList> jdReqList = tagAndFetchJDRequirements(tagger, newReqList);

			QuantifiableExperienceExtractor expExtractor = new QuantifiableExperienceExtractor();
			expExtractor.scoreQuantifiableExperienceSkill(jdReqList, foundSkills);

			KeywordBasedScore keywordBasedScore = new KeywordBasedScore();
			keywordBasedScore.assignScoreBasedOnKeyword(jdReqList, foundSkills);

			try {
				BufferedWriter bw = new BufferedWriter(
						new FileWriter("src/main/resources/scores/" + filename + ".csv"));
				bw.write("skill,score");
				bw.newLine();
				for (Requirement requirement : foundSkills) {
					for (Skill skill : requirement.getSkills()) {
						double lineScore = skill.getLineScore() == null ? 0.0 : skill.getLineScore();
						double preferenceScore = skill.getPreferenceScore() == null ? 0.0 : skill.getPreferenceScore();
						double finalScore = ((lineScore + preferenceScore) * 10) / 8;
						bw.write(skill.getSkillName() + "," + roundOff(finalScore));
						bw.newLine();
					}
				}
				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static double roundOff(double finalScore) {
		return Math.round(finalScore * 100.0) / 100.0;
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
