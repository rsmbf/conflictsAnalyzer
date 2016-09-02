package main

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class Project {

	private ArrayList<MergeScenario> mergeScenarios

	private String name

	private int analyzedMergeScenarios

	private int conflictingMergeScenarios
	
	private int conflictingScenariosOnlyNonJava
	
	private int editSameMcScenarios

	private double conflictRate

	private Map<String, Conflict> projectSummary
	
	private File mergeScenarioFile
	
	private Map<String, Integer> sameSignatureCMSummary
	
	private int possibleRenamings;
	
	private List<ProjectPeriod> periods;
	
	public Project(String projectName, List<ProjectPeriod> periods = null){
		this.mergeScenarios = new ArrayList<MergeScenario>()
		this.name = projectName
		initializeProjectSummary()
		initializeProjectMetrics()
		this.createSameSignatureCMSummary()
		this.createProjectDir()
		this.periods = periods
	}
	
	
	public void createSameSignatureCMSummary(){
		this.sameSignatureCMSummary = ConflictSummary.initializeSameSignatureCMSummary()
	}

	private void createProjectDir(){
		String projectData = "ResultData" + File.separator + this.name
		new File(projectData).mkdir()
		new File(projectData + File.separator + 'Merge_Scenarios').mkdir()
	}

	private initializeProjectMetrics() {
		this.analyzedMergeScenarios = 0
		this.conflictingMergeScenarios = 0
		this.conflictingScenariosOnlyNonJava = 0
		this.conflictRate = 0.0
		this.editSameMcScenarios = 0
	}


	public void setMergeScenarios(ArrayList<MergeScenario> ms){

		this.mergeScenarios = ms

	}

	public ArrayList<MergeScenario> getMergeScenarios(){

		return this.mergeScenarios

	}

	public void setName(String name){

		this.name = name

	}

	public String getName(){

		return this.name

	}

	public double getConflictRate(){
		return this.conflictRate
	}

	public Hashtable<String, Conflict> getProjectSummary(){
		return this.projectSummary
	}

	public SSMergeResult analyzeConflicts(String revisionFile, boolean resultGitMerge){
			
			MergeScenario ms = new MergeScenario(revisionFile, resultGitMerge)
			this.mergeScenarios.add(ms)
			ms.analyzeConflicts()
			SSMergeResult result = new SSMergeResult(ms.hasConflictsThatWereNotSolved(), ms.getFilesWithMethodsToJoana())
			updateAndPrintSummary(ms)
			ms.copyMSDirIfEditSameMc()
			ms.deleteMSDir()
			
			return result
	}

	private printResults(MergeScenario ms) {
		ConflictPrinter.printMergeScenarioReport(ms, this.name)
		ConflictPrinter.updateProjectData(this)
	}

	private void updateAndPrintSummary(MergeScenario ms){
		updateConflictingRate(ms)
		if(ms.hasConflicts){
			updateProjectSummary(ms)
			updateSameSignatureCMSummary(ms)
		}
		printResults(ms)
	}

	private updateConflictingRate(MergeScenario ms) {
		this.analyzedMergeScenarios++
		if(ms.hasConflicts){
			this.conflictingMergeScenarios++
			if(ms.hasNonDsEditSameMcConflicts())//
			{
				this.editSameMcScenarios++
			}
		}else if(ms.hasConflictsThatWereNotSolved())
		{
			this.conflictingScenariosOnlyNonJava++
		}
		this.computeConflictingRate()
	}

	private void computeConflictingRate(){

		double cr = (this.conflictingMergeScenarios/
				this.analyzedMergeScenarios) * 100
		this.conflictRate = cr.round(2)

	}

	private void initializeProjectSummary(){

		this.projectSummary = ConflictSummary.initializeConflictsSummary()

	}

	private void updateProjectSummary(MergeScenario ms){
		
		for(SSMergeConflicts c : SSMergeConflicts.values()){
			Conflict conflict = ms.getMergeScenarioSummary().get(c.toString())
			this.projectSummary = ConflictSummary.updateConflictsSummary(this.projectSummary, conflict)
			
		}
		this.possibleRenamings = this.possibleRenamings + ms.getPossibleRenamings()
	}
	
	private void updateSameSignatureCMSummary(MergeScenario ms){
		for(PatternSameSignatureCM p : PatternSameSignatureCM.values()){
			//update cause
			String cause = p.toString()
			int quantity = ms.sameSignatureCMSummary.get(cause)
			quantity = quantity + this.sameSignatureCMSummary.get(cause)
			this.sameSignatureCMSummary.put(cause, quantity)
			
			//update false positives
			String diffSpacing = cause + 'DS'
			int quantity2 = ms.sameSignatureCMSummary.get(diffSpacing)
			quantity2 = quantity2 + this.sameSignatureCMSummary.get(diffSpacing)
			this.sameSignatureCMSummary.put(diffSpacing, quantity2)
		}
	}
	
	public String toString(){
		String result = this.name + ', ' + this.analyzedMergeScenarios + ', ' +
		this.conflictingScenariosOnlyNonJava + ', ' +
		this.conflictingMergeScenarios + ', ' + 
		this.editSameMcScenarios + ', ' +
		ConflictSummary.printConflictsSummary(this.projectSummary) + ', ' +
		ConflictSummary.printSameSignatureCMSummary(this.sameSignatureCMSummary) + ', ' +
		this.possibleRenamings

		return result
	}	
	
	public List<ProjectPeriod> getProjectPeriods()
	{
		periods
	}
}
