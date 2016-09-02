package main
import java.util.List;
import java.util.HashMap;
import java.util.Hashtable

import util.CSVAnalyzer;

import org.apache.commons.io.FileUtils

import util.Util


/*this class is supposed to integrate all the 3 steps involved to run the study
 * gitminer/gremlinQuery/ConflictsAnalyzer
 */

class RunStudy {


	private String gitminerConfigProps = 'gitminerConfiguration.properties'
	private String projectName
	private String projectRepo
	private String gitminerLocation
	private String downloadPath
	private Hashtable<String, Conflict> projectsSummary

	public RunStudy(){
		ConflictPrinter.setconflictReportHeader()
	}

	public void run(String[] args){

		//read input files
		def projectsList = new File(args[0])
		updateGitMinerConfig(args[1])
		def projectsDatesFolder = args[2]
		def startYear = null
		def endYear = null
		def startIndex = 0
		def endIndex = null
		if(args != null && args.length > 3)
		{
			def yearRange = new File(args[3])
			if(yearRange.exists())
			{
				String[] splitRange = yearRange.readLines()[0].split("-")
				if(splitRange.length >= 1 && splitRange[0].length() > 0)
				{
					startYear = splitRange[0].toInteger()
				}
				if(splitRange.length == 2 && splitRange[1].length() > 0)
				{
					endYear = splitRange[1].toInteger()
				}
			}
			if(args.length > 4)
			{
				def numProjects = null
				for(int argI = 4; argI < args.length; argI++)
				{
					def fullStr = args[argI]
					if(fullStr != null){
						def splitStr = fullStr.split("=")
						if(splitStr[0] != null && splitStr[0].equals("startLine"))
						{
							startIndex = Integer.parseInt(splitStr[1]) - 1
						}else if(splitStr[0] != null && splitStr[0].equals("numProjects"))
						{
							numProjects =  Integer.parseInt(splitStr[1])
						}
					}
				}
				if(numProjects != null)
				{
					endIndex = startIndex + numProjects
				}
			}
		}
		List<String> lines = projectsList.readLines()
		
		if(endIndex == null){
			endIndex = lines.size()
		}
		 
		this.createResultDir()
		List<String> filtLines = lines.subList(startIndex, endIndex)
		//lines.remove(0)
		//for each project
		filtLines.each() {
			print it + "\n"
			//set project name
			setProjectNameAndRepo(it)

			//set projectPeriodsList
			List<ProjectPeriod> periods = getProjectPeriods(projectsDatesFolder)

			//run gitminer

			/*attention, if you have already downloaded gitminer base you can comment
			 the line below and use the second line below*/
			
			String graphBase
			if(new File(this.gitminerLocation + File.separator + this.projectName + 'graph.db').exists()){
				graphBase = this.gitminerLocation + File.separator + this.projectName + 'graph.db'
			}else{
				graphBase = runGitMiner()
			}
			//String graphBase = runGitMiner()
			//String graphBase = this.gitminerLocation + File.separator + this.projectName + 'graph.db'

			//get list of merge commits
			
			ArrayList<MergeCommit> listMergeCommits = runGremlinQuery(graphBase)
			for(MergeCommit mc : listMergeCommits)
			{
					println mc.sha
			}

			//create project and extractor
			Extractor extractor = this.createExtractor(this.projectName, graphBase)
			Project project = new Project(this.projectName, periods)

			//for each merge scenario, clone and run SSMerge on it
			analyseMergeScenario(listMergeCommits, extractor, project, startYear, endYear)

			//print project report and call R script
			ConflictPrinter.printProjectData(project)
			//this.callRScript() 
		}

	}

	private ArrayList<ProjectPeriod> getProjectPeriods(String projectsDatesFolder) {

		ArrayList<ProjectPeriod> periods = new ArrayList<ProjectPeriod>()
		def projectDatesFile = new File(projectsDatesFolder + File.separator + this.projectName + ".txt")
		if(projectDatesFile.exists())
		{
			List<String> projectPeriodsList = projectDatesFile.readLines()
			projectPeriodsList.remove(0)

			projectPeriodsList.each(){ infoLine ->
				String[] projectInfo = infoLine.split(",")
				Date startDate = null
				Date endDate = null
				String binPath = ""
				String srcPath = "/src"
				String libPaths = null
				String buildSystem = null
				if(projectInfo.length > 0 && !projectInfo[0].trim().equals(""))
				{
					startDate = Date.parse('dd/MM/yyyy', projectInfo[0])
				}

				if(projectInfo.length > 1 && !projectInfo[1].trim().equals(""))
				{
					endDate = Date.parse('dd/MM/yyyy', projectInfo[1])
				}
				if(projectInfo.length > 2 && !projectInfo[2].trim().equals("")){
					binPath = projectInfo[2].trim()
				}

				if(projectInfo.length > 3 && !projectInfo[3].trim().equals("")){
					srcPath = projectInfo[3].trim()
				}

				if(projectInfo.length > 4 && !projectInfo[4].trim().equals(""))
				{
					libPaths = projectInfo[4].trim()
				}

				if(projectInfo.length > 5 && !projectInfo[5].trim().equals(""))
				{
					buildSystem = projectInfo[5].trim()
				}
				periods.add(new ProjectPeriod(startDate, endDate, binPath, srcPath, libPaths, buildSystem))
			}
		}

		return periods
	}

	private void createResultDir(){
		File resultDir = new File ('ResultData')
		if(!resultDir.exists()){
			resultDir.mkdirs()
		}
	}

	private void analyseMergeScenario(ArrayList listMergeCommits, Extractor extractor,
			Project project, def startYear, def endYear) {
		//if project execution breaks, update current with next merge scenario number
		int current = 0//696//304//303//236//1349//1348//800//799//776//775//708//670//465//459//449//444//370//318//290//273//259//239//223//195//147
		int end = listMergeCommits.size()//237//listMergeCommits.size()
		int[] desiredIndexes = []/*694..705//*///[]

		List<ProjectPeriod> periods = project.getProjectPeriods()
		String reportsPath = new File(downloadPath).getParent() + File.separator + "reports" + File.separator + project.name
		List<Integer> indexes = new ArrayList<Integer>()
		//for each merge scenario analyze it
		while(current < end){

			int index = current + 1;
			println 'Merge scenario [' + index + '] from a total of [' + end +
					'] merge scenarios\n'
			if(desiredIndexes.size() == 0 || desiredIndexes.contains(index))
			{
				MergeCommit mc = listMergeCommits.get(current)

				MatchingProjectPeriod p = this.getPeriodMatch(periods, mc)
				def year = mc.getDate()[Calendar.YEAR]
				if(p.periodMatch && (startYear == null || year >= startYear) &&
				(endYear == null || year <= endYear))
				{
					println 'Analyzing merge scenario...'

					/*download left, right, and base revisions, performs the merge and saves in a
					 separate file*/
					ExtractorResult mergeResult = extractor.extractCommit(mc)

					String revisionFile = mergeResult.getRevisionFile()

					if(!revisionFile.equals("")){
						try{
							//run ssmerge and conflict analysis
							SSMergeResult ssMergeResult = runConflictsAnalyzer(project, revisionFile,
									mergeResult.getNonJavaFilesWithConflict().isEmpty())

							boolean hasConflicts = ssMergeResult.getHasConflicts()
							println hasConflicts
							if(!hasConflicts){
								//get line of the files containing methods for joana analysis
								Map<String, ArrayList<MethodEditedByBothRevs>> filesWithMethodsToJoana =
										ssMergeResult.getFilesWithMethodsToJoana()
								if(filesWithMethodsToJoana.size() > 0)
								{
									String revPath = revisionFile.replace(".revisions", "")
									indexes.add(index)
									println index + ", " + filesWithMethodsToJoana.keySet()
									File editSameMCContribs = new File(reportsPath + File.separator + "editSameMCcontribs.csv")
									new File(reportsPath).mkdirs()
									editSameMCContribs.createNewFile()
									if(editSameMCContribs.readLines().empty)
									{
										editSameMCContribs.append "Index; Revision; Date; File; Signature; Number of Lines; Left; Right" + "\n"
									}
									//List<String> methodsList = new ArrayList<String>()
									for(String file : filesWithMethodsToJoana.keySet())
									{
										for(MethodEditedByBothRevs method : filesWithMethodsToJoana.get(file))
										{
											//methodsList.add(method.getSignature() + " - LENGTH: " + method.getLength() + " - LEFT: "+method.getLeftLines() + " - RIGHT: "+method.getRightLines())
											editSameMCContribs.append index + "; "+ new File(revPath).getName() + "; " + mc.date + "; "
											editSameMCContribs.append file + "; " + method.getSignature() + "; " + method.getLength() + "; "
											editSameMCContribs.append method.getLeftLines().toListString() + "; " + method.getRightLines().toListString()	+ "\n"
										}
									}
									/*
									 editSameMCContribs.append index + " - "+new File(revPath).getName()+ " - Date: "+mc.date+ " - Size: "+methodsList.size() +"\n"
									 for(String method : methodsList){
									 editSameMCContribs.append "	"+method +"\n"
									 }
									 */
									/*
									 String reportsFilePath = reportsPath + File.separator + (new File(revPath).getName())
									 File reportsRevDir = new File(reportsFilePath)
									 reportsRevDir.deleteDir()
									 reportsRevDir.mkdirs()
									 File emptyContribs = new File(reportsFilePath + File.separator + "emptyContributions.txt")
									 emptyContribs.createNewFile()
									 */
									//Map ssmerge objects to joana objects

									/*
									 Map<String, ModifiedMethod> methodsMap = getJoanaMap(emptyContribs, filesWithMethodsToJoana)
									 if(emptyContribs.length() == 0)
									 {
									 emptyContribs.delete()
									 }
									 if(methodsMap.size() > 0)
									 {
									 /*
									 String revGitPath = revPath + File.separator + "git"
									 File revGitFile = new File(revGitPath)
									 */

									//copy revision folder
									/*
									 def repoDir = new File(downloadPath +File.separator+ projectName + File.separator + "git")
									 File revEditSameMc = new File(revPath.replace("revisions","editsamemc_revisions"))
									 FileUtils.copyDirectory(new File(new File(revPath).getParent()), new File(revEditSameMc.getParent()))
									 File revGitEditSameMc = new File(revEditSameMc.absolutePath + File.separator + "git")
									 FileUtils.copyDirectory(new File(revPath), revGitEditSameMc)
									 copyGitFiles(repoDir, repoDir, revGitEditSameMc)
									 */

									/*
									 copyGitFiles(repoDir, repoDir, revEditSameMc)
									 String base 
									 int i = 0
									 File revParent = new File(revEditSameMc.getParent())
									 while(i < revParent.listFiles().length && 
									 (!revParent.listFiles()[i].isDirectory() || !revParent.listFiles()[i].name.contains("rev_base")))
									 {										
									 i++;
									 }
									 if(i < revParent.listFiles().length)
									 {
									 base = revParent.listFiles()[i].name
									 copyGitFiles(repoDir, repoDir, new File(revParent.absolutePath + File.separator + base))
									 }
									 String leftANDright = revEditSameMc.name.replace("rev_", "")
									 String left = "rev_left_"+leftANDright.substring(0,5)
									 String right = "rev_right_"+leftANDright.substring(6)
									 copyGitFiles(repoDir, repoDir, new File(revParent.absolutePath + File.separator + left))
									 copyGitFiles(repoDir, repoDir, new File(revParent.absolutePath + File.separator + right))
									 */
									/*
									 FileUtils.copyDirectory(new File(revPath), revGitFile)
									 copyGitFiles(repoDir, repoDir, revGitFile)
									 File buildResultFile = new File(reportsFilePath + File.separator + "build_report.txt")
									 buildResultFile.createNewFile()
									 if(build(p.period.getBuildSystem(),revGitPath, buildResultFile))
									 {
									 //call joana analysis
									 println "Calling Joana"
									 JoanaInvocation joana = new JoanaInvocation(revGitPath, methods, p.period.getBinPath(),
									 p.period.getSrcPath(), p.period.getLibPaths(), reportsFilePath)
									 joana.run()
									 }
									 }
									 */
								}
							}
						}catch(Exception e)
						{
							e.printStackTrace()
						}
					}
				}
			}
			//increment current
			current++

		}
		println indexes
	}

	private MatchingProjectPeriod getPeriodMatch(List<ProjectPeriod> periods, MergeCommit mc){
		boolean periodMatch = false
		ProjectPeriod period = null
		if(periods.size() > 0)
		{
			int currentPeriod = 0

			Date startDate = null
			Date finalDate = null

			while(currentPeriod < periods.size() && !periodMatch)
			{
				period = periods[currentPeriod]
				startDate = period.getStartDate()
				finalDate = period.getEndDate()
				periodMatch = (startDate == null || mc.date.clearTime() >= startDate) &&
						(finalDate == null || mc.date.clearTime() <= finalDate)
				if(!periodMatch)
				{
					currentPeriod++
				}
			}
		}else{
			periodMatch = true
		}

		MatchingProjectPeriod result = new MatchingProjectPeriod(periodMatch, period)
		return result
	}
/*
	private Map getJoanaMap(File emptyContributions,Map filesWithMethodsToJoana) {
		Map<String, ModifiedMethod> methods = new HashMap<String, ModifiedMethod>()
		for(String file : filesWithMethodsToJoana.keySet()) {
			for(MethodEditedByBothRevs method : filesWithMethodsToJoana.get(file)){
				if(method.leftLines.size > 0 && method.rightLines.size > 0)
				{
					List<String> constArgs;
					def constructor = method.getConstructor()
					if(constructor != null)
					{
						constArgs = Util.getArgs(Util.simplifyMethodSignature(constructor.getName()));
					}else {
						constArgs = new ArrayList<String>()
					}
					methods.put(method.getSignature(), new ModifiedMethod(method.getSignature(), constArgs, method.getLeftLines(), method.getRightLines(), method.getImportsList()))
				}else{
					println "One or more empty contributions on: "+method.getSignature()
					emptyContributions.append("One or more empty contributions on: "+method.getSignature()+"\n")
					emptyContributions.append("   Left Contribution:"+method.leftLines+"\n")
					emptyContributions.append("   Right Contribution:"+method.rightLines+"\n")
					emptyContributions.append("\n")
				}
			}
		}
		return methods
	}
*/
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
/*
	private boolean build(String fullBuildSystem, String revGitPath, File buildResultFile) {
		println "Building..."
		int lastSeparator = fullBuildSystem.lastIndexOf(File.separator) + 1
		String buildSystem = fullBuildSystem.substring(lastSeparator)
		String buildSystemLocation = fullBuildSystem.substring(0, lastSeparator)
		def buildCmd = buildSystemLocation + File.separator
		if(buildSystem.equals("gradlew"))
		{
			def gradlewPath = revGitPath + File.separator+"gradlew"
			buildCmd = "chmod +x "+gradlewPath + " && "+gradlewPath+" build -p"+revGitPath //+" -x test"
		}else if(buildSystem.equals("gradle"))
		{
			buildCmd += "gradle build -p"+revGitPath/*+" -x test"* /
		}else if(buildSystem.equals("ant"))
		{
			buildCmd += "ant build -buildfile "+ revGitPath + File.separator +"build.xml"
		}else if(buildSystem.equals("mvn")){
			buildCmd += "mvn compile "+ revGitPath + File.separator +"pom.xml"
		}
		//start here
		ProcessBuilder builder = new ProcessBuilder("/bin/bash","-c",buildCmd);
		builder.redirectErrorStream(true);
		Process p = builder.start();
		BufferedReader buffer 	= new BufferedReader(new InputStreamReader(p.getInputStream()));
		String currentLine 		= "";
		List<String> buildLines = new ArrayList<String>()
		while ((currentLine=buffer.readLine())!=null) {
			buildLines.add(currentLine)
			buildResultFile.append(currentLine+"\n")
			println currentLine
		}
		int i = buildLines.size() - 1
		while(i >= 0 && !buildLines.get(i).equals("BUILD SUCCESSFUL") && !buildLines.get(i).equals("BUILD FAILED"))
		{
			i--;
		}
		return i >= 0 && buildLines.get(i).equals("BUILD SUCCESSFUL")
	}
*/
	private Extractor createExtractor(String projectName, String graphBase){
		GremlinProject gProject = new GremlinProject(this.projectName,
				this.projectRepo, graphBase)
		Extractor extractor = new Extractor(gProject, this.downloadPath)

		return extractor
	}

	public Hashtable<String, Conflict> getProjectsSummary(){
		return this.projectsSummary
	}

	public void updateGitMinerConfig(String configFile){
		Properties gitminerProps =  new Properties()
		File gitminerPropsFile = new File(this.gitminerConfigProps)
		gitminerProps.load(gitminerPropsFile.newDataInputStream())


		Properties configProps = new Properties()
		File propsFile = new File(configFile)
		configProps.load(propsFile.newDataInputStream())

		this.gitminerLocation = configProps.getProperty('gitminer.path')
		this.downloadPath = configProps.getProperty('downloads.path')
		String graphDb = this.gitminerLocation + File.separator + 'graph.db'
		String repo_Loader = this.gitminerLocation + File.separator + 'repo_loader'

		gitminerProps.setProperty('net.wagstrom.research.github.dburl', graphDb)
		gitminerProps.setProperty('edu.unl.cse.git.localStore', repo_Loader)
		gitminerProps.setProperty('edu.unl.cse.git.repositories', graphDb)
		gitminerProps.setProperty('edu.unl.cse.git.dburl', graphDb)

		gitminerProps.setProperty('net.wagstrom.research.github.login', configProps.getProperty('github.login'))
		gitminerProps.setProperty('net.wagstrom.research.github.password', configProps.getProperty('github.password'))
		gitminerProps.setProperty('net.wagstrom.research.github.email', configProps.getProperty('github.email'))
		gitminerProps.setProperty('net.wagstrom.research.github.token', configProps.getProperty('github.token'))

		gitminerProps.store(gitminerPropsFile.newWriter(), null)

	}

	public void setProjectNameAndRepo(String project){
		String[] projectData = project.split('/')
		this.projectName = projectData[1].trim()
		this.projectRepo = project
		println "Starting project " + this.projectName
	}


	public String runGitMiner(){
		updateProjectRepo()
		println "Running gitminer"
		runGitminerCommand('./gitminer.sh')
		runGitminerCommand('./repository_loader.sh')
		String graphBase = renameGraph()
		println "Finished running gitminer"
		return graphBase
	}


	private String renameGraph(){

		String oldFile = this.gitminerLocation + File.separator + 'graph.db'
		String newFile = this.gitminerLocation + File.separator + this.projectName + 'graph.db'
		new File(oldFile).renameTo(new File(newFile))

		return newFile
	}

	public void runGitminerCommand(String command){
		String propsFile = new File("").getAbsolutePath() + File.separator + this.gitminerConfigProps
		ProcessBuilder pb = new ProcessBuilder(command, "-c", propsFile)
		pb.directory(new File(this.gitminerLocation))
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		// Start the process.
		try {
			Process p = pb.start()
			p.waitFor()
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void updateProjectRepo(){
		Properties gitminerProps = new Properties()
		File gitminerPropsFile = new File(this.gitminerConfigProps)
		gitminerProps.load(gitminerPropsFile.newDataInputStream())
		gitminerProps.setProperty('net.wagstrom.research.github.projects', this.projectRepo)
		gitminerProps.setProperty('edu.unl.cse.git.repositories', this.projectRepo)
		gitminerProps.store(gitminerPropsFile.newWriter(), null)
	}

	public ArrayList<MergeCommit> runGremlinQuery(String graphBase){
		println "starting to query the gremlin database and download merge revision"
		GremlinQueryApp gq = new GremlinQueryApp()
		ArrayList<MergeCommit> listMergeCommits = gq.run(projectName, projectRepo, graphBase)
		return listMergeCommits
	}

	public SSMergeResult runConflictsAnalyzer(Project project, String revisionFile, boolean resultGitMerge){
		println "starting to run the conflicts analyzer on revision " + revisionFile
		SSMergeResult result = project.analyzeConflicts(revisionFile, resultGitMerge)
		return result
	}

	public void callRScript(){

		CSVAnalyzer.writeRealConflictsCSV()
		String propsFile = "resultsScript.r"
		ProcessBuilder pb = new ProcessBuilder("Rscript", propsFile)
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		// Start the process.
		try {
			Process p = pb.start()
			p.waitFor()
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main (String[] args){
		RunStudy study = new RunStudy()
		String[] files= ['projectsList', 'configuration.properties',
			'ProjectsDatesInfo','yearRange'/*,'startLine=1','numProjects=10'*/]
		String[] fullArgs = files + args
		study.run(fullArgs) 
		//println study.build("/usr/local/bin/ant", "/Users/Roberto/Documents/UFPE/Msc/Projeto/projects/temp/voldemort", new File("/Users/Roberto/Documents/UFPE/Msc/Projeto/projects/temp/report.txt"))
		//println study.build("/usr/local/bin/mvn", "/Users/Roberto/Documents/UFPE/Msc/Projeto/projects/temp/jedis", new File("/Users/Roberto/Documents/UFPE/Msc/Projeto/projects/temp/report.txt"))
	}

}
