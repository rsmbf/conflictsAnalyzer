package conflictsAnalyzer

import java.util.ArrayList;

public class ConflictPrinter {

	public void writeConflicts(ArrayList<Conflict> conflictsList){}


	public void printConflictsReport(Hashtable<String, Integer> conflictsReport, String revisionFilePath){

		def out = new File('conflictReport.csv')

		out.append('Revision: ' + revisionFilePath + '\n')

		Set<String> keys = conflictsReport.keySet();
		for(String key: keys){

			def row = [key+": "+ conflictsReport.get(key)]
			out.append row.join(',')
			out.append '\n'

		}

	}

	def printConflictsList(ArrayList<Conflict> conflictsList, String revisionFilePath){

		def out = new File('conflictList.csv')

		def delimiter = '========================================================='
		out.append(delimiter)
		out.append '\n'

		out.append('Revision: ' + revisionFilePath + '\n')


		for(Conflict conflict : conflictsList){

			def row = ['Conflict type: '+ conflict.getType() + '\n' + 'Conflict body: ' + '\n' +conflict.getBody() ]
			out.append row.join(',')
			out.append '\n'
			row = ['File path: ' + conflict.getFilePath()]
			out.append row.join(',')
			out.append '\n'

		}

		out.append '\n'
		out.append(delimiter)

	}

	//the methods below that will be used after restructuring the architecture
	public static void printProjectsReport(ArrayList<Project> projects){

		for(Project p: projects){

		}


	}
	
	public static void printProjectReport(Project project){
		String fileName = 'Project' + project.getName() + 'Report.csv'
		def out = new File(fileName)
		
		// deleting old files if it exists
		out.delete()
		
		out = new File(fileName)
		
		Set<String> keys = project.projectSummary.keySet
		for(String key: keys){
			
			def row = [key+": "+ project.projectSummary.get(key)]
			out.append row.join(',')
			out.append '\n'
			
		}
	}
	
	public static void printMergeScenarioReport(MergeScenario mergeScenario){
		def out = new File('MergeScenariosReport.csv')

		out.append('Merge scenario: ' + mergeScenario.path + '\n')

		Set<String> keys = mergeScenario.mergeScenarioSummary.keySet();
		for(String key: keys){

			def row = [key+": "+ mergeScenario.mergeScenarioSummary.get(key)]
			out.append row.join(',')
			out.append '\n'

		}

		printConflictsReport(mergeScenario.getConflicts(), mergeScenario.path)
	}

	public static void printConflictsReport(ArrayList<Conflict> conflicts, String mergeScenarioPath){

		def out = new File('ConflictsReport.csv')

		def delimiter = '========================================================='
		out.append(delimiter)
		out.append '\n'

		out.append('Revision: ' + mergeScenarioPath + '\n')
		for(Conflict c: conflicts){

				def row = ['Conflict type: '+ c.getType() + '\n' + 'Conflict body: ' + '\n' + c.getBody() ]
				out.append row.join(',')
				out.append '\n'
				row = ['File path: ' + c.getFilePath()]
				out.append row.join(',')
				out.append '\n'
			
		}
		out.append '\n'
		out.append(delimiter)
	}
}
