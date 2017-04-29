package tcd.edu.skillextractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tcd.edu.skillextractor.bean.JDRequirementList;
import tcd.edu.skillextractor.bean.Requirement;
import tcd.edu.skillextractor.bean.ScoredKeyword;
import tcd.edu.skillextractor.bean.Skill;

public class KeywordBasedScore {

	public void assignScoreBasedOnKeyword(List<JDRequirementList> reqList, List<Requirement> foundSkills) {
		List<ScoredKeyword> scoredKeywords = extractScoredKeyword();

		for (JDRequirementList jdReq : reqList) {
			if (jdReq.getStatus() == 1)
				continue;

			for (ScoredKeyword scoredKeyword : scoredKeywords) {
				for (String keyword : scoredKeyword.getKeywords()) {
					if (jdReq.getRequirementString().contains(keyword)) {
						jdReq.setStatus(1);
						break;
					}
				}
				if (jdReq.getStatus() == 1) {
					assignScoreBasedOnKeyword(foundSkills, jdReq, scoredKeyword.getScore());
					break;
				}
			}
		}

	}

	private void assignScoreBasedOnKeyword(List<Requirement> foundSkills, JDRequirementList jdReq, double score) {
		for (Requirement requirement : foundSkills) {
			for (Skill skill : requirement.getSkills()) {
				if (skill.getLineNo() == jdReq.getLineIndex()) {
					skill.setPreferenceScore(score);
				}
			}
		}
	}

	private List<ScoredKeyword> extractScoredKeyword() {
		BufferedReader br = null;
		List<ScoredKeyword> scoredKeywords = new ArrayList<ScoredKeyword>();
		try {
			br = new BufferedReader(new FileReader("src/main/resources/scoredKeywords"));
			String line = "";
			while ((line = br.readLine()) != null) {
				ScoredKeyword scoredKeyword = new ScoredKeyword();
				scoredKeyword.setScore(Double.parseDouble(line.split(",")[0]));
				scoredKeyword.setKeywords(Arrays.asList(line.split(",")[1].split(":")));
				scoredKeywords.add(scoredKeyword);
			}
			
			br.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return scoredKeywords;
	}
}
