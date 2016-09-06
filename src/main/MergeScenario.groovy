package main


import java.io.File;
import java.util.LinkedList
import java.util.Map;
import java.util.Observable;

import merger.FSTGenMerger;
import merger.MergeVisitor
import sun.tools.jar.Main;
import util.CompareFiles;
import composer.rules.ImplementsListMerging
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

import org.apache.commons.io.FileUtils

class MergeScenario implements Observer {

	private String path

	private String name

	private ArrayList<MergedFile> mergedFiles

	private Map<String,Conflict> mergeScenarioSummary

	private boolean hasConflicts

	private CompareFiles compareFiles

	private FSTGenMerger fstGenMerge

	private Map<String, Integer> sameSignatureCMSummary

	private int possibleRenamings

	private int filesAddedByOneDev

	private boolean gitMergeHasNoConflicts
	
	private Map<String, ArrayList<MethodEditedByBothRevs>> filesWithMethodsToJoana

	public MergeScenario(String path, boolean resultGitMerge){

		this.path = path
		this.gitMergeHasNoConflicts = resultGitMerge
		this.setName()
		//this.removeVarArgs()
		this.hasConflicts = false
		this.createMergeScenarioSummary()
		this.createSameSignatureCMSummary()
		this.setMergedFiles()
		this.filesWithMethodsToJoana = new HashMap<String, ArrayList<MethodEditedByBothRevs>>()
	}

	public void createSameSignatureCMSummary(){
		this.sameSignatureCMSummary = ConflictSummary.initializeSameSignatureCMSummary()
	}

	public void setMergedFiles(){
		this.compareFiles = new CompareFiles(this.path)
		this.compareFiles.ignoreFilesWeDontMerge()
		this.mergedFiles = this.compareFiles.getFilesToBeMerged()
	}

	public ArrayList<MergedFile> getMergedFiles(){
		return this.mergedFiles
	}

	public HashMap<String, Conflict> getMergeScenarioSummary(){
		return this.mergeScenarioSummary
	}
	
	public boolean hasNonDsEditSameMcConflicts()
	{
		def type = SSMergeConflicts.EditSameMC.toString()
		def editSameMc = this.mergeScenarioSummary.get(type)
		int diff =  editSameMc.getNumberOfConflicts() - editSameMc.getDifferentSpacing()
		return diff > 0
	}

	public void setName(){
		String [] temp = this.path.split('/')
		String revFile = temp[temp.length -1]
		this.name = revFile.substring(0, revFile.length()-10)
	}

	public String getName(){
		return this.name
	}

	public void analyzeConflicts(){

		this.runSSMerge()
		this.assignLeftAndRight()
		this.compareFiles.restoreFilesWeDontMerge()
	}
	
	public void assignLeftAndRight(){
		for(String filePath : this.filesWithMethodsToJoana.keySet()){
			ArrayList<MethodEditedByBothRevs> methods = this.filesWithMethodsToJoana.get(filePath)
			for(MethodEditedByBothRevs method : methods ){
				method.assignLeftAndRight()
			}
		}
	}

	public void deleteMSDir(){
		String msPath = this.path.substring(0, (this.path.length()-26))
		File dir = new File(msPath)
		boolean deleted = dir.deleteDir()
		if(deleted){
			println 'Merge scenario ' + this.path + ' deleted!'
		}else{

			println 'Merge scenario ' + this.path + ' not deleted!'
		}
	}
	
	public void copyMSDirIfEditSameMc()
	{
		/*
		def repoDir = new File(downloadPath +File.separator+ projectName + File.separator + "git")
		File revEditSameMc = new File(revPath.replace("revisions","editsamemc_revisions"))
		FileUtils.copyDirectory(new File(new File(revPath).getParent()), new File(revEditSameMc.getParent()))
		File revGitEditSameMc = new File(revEditSameMc.absolutePath + File.separator + "git")
		FileUtils.copyDirectory(new File(revPath), revGitEditSameMc)
		copyGitFiles(repoDir, repoDir, revGitEditSameMc)*/
	
		if(!hasConflictsThatWereNotSolved() && filesWithMethodsToJoana.size() > 0)
		{
			String revPath = path.replace(".revisions","")
			String revPathParent = new File(revPath).getParent()
			String revMergedPath = revPathParent + File.separator + "rev_merged_git"
			File repoDir = new File(new File(revPathParent).getParent().replace("revisions", "git"))
			File revEditSameMc = new File(revPath.replace("revisions","editsamemc_revisions"))
			File revEditSameMcParent = new File(revEditSameMc.getParent())
			FileUtils.copyDirectory(new File(revPathParent), revEditSameMcParent)
			File revGitEditSameMc = new File(revEditSameMc.absolutePath + File.separator + "git")
			FileUtils.copyDirectory(new File(revPath), revGitEditSameMc)
			copyGitFiles(repoDir, repoDir, revGitEditSameMc)
			File revEditSameMcMerged = new File(revEditSameMcParent.absolutePath + File.separator + "rev_merged_git")
			File revGitEditSameMcMerged = new File(revEditSameMcMerged.absolutePath + File.separator + "git")
			FileUtils.copyDirectory(new File(revMergedPath), revGitEditSameMcMerged)
			copyGitFiles(repoDir, repoDir, revGitEditSameMcMerged)
		}
	}
	
	private def copyGitFiles(File baseDir, File srcDir, File destDir)
	{
		String basePath = baseDir.getAbsolutePath()
		String destPath = destDir.getAbsolutePath()
		File[] srcFiles = srcDir.listFiles()
		for(File file : srcFiles)
		{
			if(file.getName().contains(".git"))
			{
				if(file.isFile())
				{
					FileUtils.copyFile(file, new File(file.getAbsolutePath().replace(basePath, destPath)))
				}else if(file.isDirectory())
				{
					FileUtils.copyDirectory(file, new File(file.getAbsolutePath().replace(basePath, destPath)))
				}
			}
		}
	}

	public void runSSMerge(){
		this.fstGenMerge = new FSTGenMerger()
		fstGenMerge.getMergeVisitor().addObserver(this)
		String[] files = ["--expression", this.path]
		fstGenMerge.run(files)

	}


	public void createMergeScenarioSummary(){
		this.mergeScenarioSummary = ConflictSummary.initializeConflictsSummary()
	}

	public void updateMergeScenarioSummary(Conflict conflict){
		this.mergeScenarioSummary = ConflictSummary.updateConflictsSummary(this.mergeScenarioSummary
				, conflict)
		this.possibleRenamings = this.possibleRenamings + conflict.getPossibleRenaming()
	}

	public boolean getHasConflicts(){
		return this.hasConflicts
	}

	public void removeVarArgs(){
		String OS = System.getProperty("os.name").toLowerCase()
		String sSed = ""
		if (OS.contains('mac')){
			sSed = "xargs sed -i \'\' s/\\.\\.\\./[]/g"
		}else if(OS.contains('linux')){
			sSed = "xargs sed -i s/\\.\\.\\./[]/g"
		}
		String msPath = this.path.substring(0, (this.path.length()-26))
		String command = "grep -rl ... " + msPath
		def procGrep = command.execute()
		def procSed = sSed.execute()
		procGrep | procSed
		procSed.waitFor()
	}

	@Override
	public void update(Observable o, Object arg) {

		if(o instanceof MergeVisitor && arg instanceof FSTTerminal){

			FSTTerminal node = (FSTTerminal) arg

			if(!node.getType().contains("-Content")){
				
				if(this.isMethodWithoutConflicts(node.getBody())){	
					
					this.createMethodToJoana(node)
					
				}else{
				
				if(!this.hasConflicts){
					this.hasConflicts = true
					this.removeNonMCBaseNodes(fstGenMerge.baseNodes)
				}

				this.createConflict(node)
				
				}
				

			}
		}
	}

	private createMethodToJoana(FSTTerminal arg) {
		MethodEditedByBothRevs method = new MethodEditedByBothRevs(arg, this.path)
		String filePath = method.getFilePath()
		ArrayList<MethodEditedByBothRevs> methods = this.filesWithMethodsToJoana.get(filePath)

		if(methods == null){
			methods = new ArrayList<MethodEditedByBothRevs>()

		}

		methods.add(method)
		this.filesWithMethodsToJoana.put(filePath, methods)
	
	}

	public void createConflict(FSTTerminal node){
		Conflict conflict = new Conflict(node, this.path);
		this.matchConflictWithFile(conflict)
		this.updateMergeScenarioSummary(conflict)

	}

	private void updateSameSignatureCMSummary(String cause, int ds){
		this.sameSignatureCMSummary = ConflictSummary.
				updateSameSignatureCMSummary(this.sameSignatureCMSummary, cause, ds)
	}

	private void matchConflictWithFile(Conflict conflict){
		String rev_base = this.compareFiles.baseRevName
		String conflictPath = conflict.filePath
		boolean matchedFile = false
		int i = 0
		while(!matchedFile && i < this.mergedFiles.size){
			String mergedFilePath = this.mergedFiles.elementData(i).path.replaceFirst(rev_base, this.name)
			if(conflictPath.equals(mergedFilePath)){
				matchedFile = true
				boolean addedByOneDev = this.mergedFiles.get(i).isAddedByOneDev()
				this.addConflictToFile(conflict, i, addedByOneDev)
			}else{
				i++
			}
		}

		if(!matchedFile){
			MergedFile mf = new MergedFile(conflict.getFilePath())
			mf.setAddedByOneDev(true)
			this.mergedFiles.add(mf)
			this.filesAddedByOneDev++
			this.addConflictToFile(conflict, this.mergedFiles.size-1, true)
		}

	}

	private void addConflictToFile(Conflict conflict, int index, boolean matched){

		if(conflict.getType().equals(SSMergeConflicts.SameSignatureCM.toString())){

			conflict.setCauseSameSignatureCM(fstGenMerge.baseNodes, matched)
			String cause = conflict.getCauseSameSignatureCM()
			this.updateSameSignatureCMSummary(cause, conflict.getDifferentSpacing())
		}

		this.mergedFiles.elementData(index).conflicts.add(conflict)
		this.mergedFiles.elementData(index).updateMetrics(conflict)

	}

	public String printMetrics(){
		String result = ''
		for(MergedFile m : this.mergedFiles){
			if(m.conflicts.size != 0){
				result = result + m.toString()
			}
		}
		return result
	}

	private int getNumberOfFilesWithConflicts(){
		int result = 0
		for(MergedFile m : this.mergedFiles){
			if(m.hasConflicts()){
				result = result + 1
			}
		}
		return result
	}

	public String toString(){
		String report = this.name + ', ' + this.compareFiles.getNumberOfTotalFiles() +
				', ' + this.compareFiles.getFilesEditedByOneDev() + ', ' +
				this.compareFiles.getFilesThatRemainedTheSame() + ', ' +
				this.filesAddedByOneDev +', ' + this.mergedFiles.size() +
				', ' + !this.gitMergeHasNoConflicts +
				', ' + this.getNumberOfFilesWithConflicts() + ', ' + 		
				ConflictSummary.printConflictsSummary(this.mergeScenarioSummary) + ', ' +
				ConflictSummary.printSameSignatureCMSummary(this.sameSignatureCMSummary) + ', ' +
				this.possibleRenamings

		return report
	}

	private void removeNonMCBaseNodes(LinkedList<FSTNode> bNodes){
		LinkedList<FSTNode> baseNodes = new LinkedList<FSTNode>(bNodes)
		for(FSTNode baseNode: baseNodes){
			if(!(baseNode.getType().equals("MethodDecl") || baseNode.getType().equals("ConstructorDecl"))){
				bNodes.remove(baseNode)
			}
		}
	}

	public int getPossibleRenamings() {
		return possibleRenamings;
	}

	public void setPossibleRenamings(int possibleRenamings) {
		this.possibleRenamings = possibleRenamings;
	}

	public boolean hasConflictsThatWereNotSolved(){
		boolean result = false

		if(this.gitMergeHasNoConflicts){
			result = this.hasNonDSConflicts()
		}else{
			result = true
		}

		return result
	}

	private boolean hasNonDSConflicts(){
		boolean hasNonDSConflict = false

		int i = 0

		while((!hasNonDSConflict) && (i < SSMergeConflicts.values().length)){
			String type = SSMergeConflicts.values()[i].toString()
				
				Conflict conflict = this.mergeScenarioSummary.get(type)
				int diff =  conflict.getNumberOfConflicts() - conflict.getDifferentSpacing()
				
				if(diff >0){
					hasNonDSConflict = true
				}
			i++
		}

		return hasNonDSConflict
	}
	
	public Map<String, ArrayList<MethodEditedByBothRevs>> getFilesWithMethodsToJoana() {
		return filesWithMethodsToJoana;
	}

	public void setFilesWithMethodsToJoana(Map<String, ArrayList<MethodEditedByBothRevs>> filesWithMethodsToJoana) {
		this.filesWithMethodsToJoana = filesWithMethodsToJoana;
	}

	private boolean isMethodWithoutConflicts(String nodeBody){
		boolean result = false
		
		if(nodeBody.contains(Blame.LEFT_SEPARATOR) || nodeBody.contains(Blame.RIGHT_SEPARATOR)){
			result = true
		}
		
		return result
	}

	public static void main(String[] args){
		MergeScenario ms = new MergeScenario('/Users/Roberto/Desktop/Teste/rev.revisions', true)
		ms.analyzeConflicts()
		/*Map <String,Conflict> mergeScenarioSummary = new HashMap<String, Conflict>()
		 String type = SSMergeConflicts.EditSameMC.toString()
		 mergeScenarioSummary.put(type, new Conflict(type))
		 Conflict conflict = mergeScenarioSummary.get(type)
		 conflict.setNumberOfConflicts(5);
		 println 'hello world'*/

	}

}
