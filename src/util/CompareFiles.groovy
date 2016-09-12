package util

import java.io.File;

import org.apache.commons.io.FileUtils

import main.MergedFile;

class CompareFiles {

	private String leftRevName

	private String baseRevName

	private String rightRevName

	private String revDir

	private File tempDir

	private int filesEditedByOneDev

	private int filesThatRemainedTheSame

	private ArrayList<MergedFile> filesToBeMerged

	public CompareFiles(String revFile){

		this.setDirNames(revFile)
		this.filesToBeMerged = new ArrayList<MergedFile>()
	}

	private void setDirNames(String revFile){
		this.revDir = new File(revFile).getParent()
		String[] revs = new File(revFile).text.split('\n')
		this.leftRevName = revs[0]
		this.baseRevName = revs[1]
		this.rightRevName = revs[2]
		this.tempDir = new File(this.revDir + File.separator + 'temp')


	}

	public ArrayList<MergedFile> getFilesToBeMerged(){
		return this.filesToBeMerged
	}

	public void ignoreFilesWeDontMerge(){
		//delete non java files
		this.removeNonJavaFiles();

		//move files that remained the same or only one version differs
		String baseFolder = this.revDir + File.separator + this.baseRevName
		this.iterateRevFolders(this.leftRevName, this.baseRevName, baseFolder, this.rightRevName)
	}
		
	private void iterateRevFolders(String leftRevName, String baseRevName, String baseFolder, String rightRevName){

		File directory = new File(baseFolder)
		if(directory.exists()){
			File[] fList = directory.listFiles()
			for (File file : fList){
				if (file.isDirectory()){
					iterateRevFolders(leftRevName, baseRevName, file.getAbsolutePath(), rightRevName)
				} else {
					String leftFilePath   = file.getAbsolutePath().replaceFirst(baseRevName, leftRevName)
					String rightFilePath  = file.getAbsolutePath().replaceFirst(baseRevName, rightRevName)
					this.compareAndMoveFiles(leftFilePath, file.getAbsolutePath() ,rightFilePath)
				}
			}
		}
	}


	private void compareAndMoveFiles(String leftFile, String baseFile, String rightFile){

		File left = new File(leftFile)
		File base = new File(baseFile)
		File right = new File(rightFile)

		if(left.exists() && base.exists() && right.exists()){
			this.compareFiles(left, base, right)
		}

	}

	private void compareFiles (File left, File base, File right){

		boolean leftEqualsBase = FileUtils.contentEquals(left, base)
		boolean rightEqualsBase = FileUtils.contentEquals(right, base)

		if(leftEqualsBase && rightEqualsBase){
			this.filesThatRemainedTheSame = this.filesThatRemainedTheSame + 1
			this.moveAndDeleteFiles(this.baseRevName, base, left, right)

		}else if((!leftEqualsBase) && rightEqualsBase){
			this.filesEditedByOneDev = this.filesEditedByOneDev + 1
			this.moveAndDeleteFiles(this.leftRevName, left, base, right)

		}else if(leftEqualsBase && (!rightEqualsBase)){
			this.filesEditedByOneDev = this.filesEditedByOneDev + 1
			this.moveAndDeleteFiles(this.rightRevName, right, base, left)

		}else if((!leftEqualsBase) && (!rightEqualsBase)){
			MergedFile mf = new MergedFile(base.getAbsolutePath())
			this.filesToBeMerged.add(mf)
		}

	}

	private void moveAndDeleteFiles(String revName, File toBeMoved, File toBeDeleted1 = null, File toBeDeleted2 = null){

		String temp = toBeMoved.getAbsolutePath().replaceFirst(revName, 'temp2')
		FileUtils.moveFile(toBeMoved, new File(temp))
		if(toBeDeleted1 != null)
		{
			FileUtils.forceDelete(toBeDeleted1)
		}
		if(toBeDeleted2 != null)
		{
			FileUtils.forceDelete(toBeDeleted2)
		}
	}

	public int getNumberOfTotalFiles(){

		int totalFiles = this.filesEditedByOneDev + this.filesThatRemainedTheSame + this.filesToBeMerged.size()
		return totalFiles
	}

	public void restoreFilesWeDontMerge(){

		//copy non java files from rev_merged_git dir
		File sourcedir = new File(this.revDir + File.separator + 'rev_merged_git');
		this.moveFiles(sourcedir)

		//copy java files from temp2
		sourcedir = new File(this.revDir + File.separator + 'temp2');
		if(sourcedir.exists()){
			this.moveFiles(sourcedir)
		}
		
	}

	private void moveFiles(File sourceDir){
		File[] files = sourceDir.listFiles()

		for(File file : files){

			if(file.isDirectory()){
				this.moveFiles(file)
			}else{
				this.auxMoveFiles(file)
			}
		}
	}

	private void auxMoveFiles(File sourceDir){
		String temp = ''
		String leftId = this.leftRevName.length() > 5 ? this.leftRevName.substring(this.leftRevName.length() - 5) : this.leftRevName
		String rightId = this.rightRevName.length() > 5 ? this.rightRevName.substring(this.rightRevName.length() - 5) : this.rightRevName
		String revName = 'rev_' + leftId + '-' + rightId
		
		String source = sourceDir.getAbsolutePath()

		if(source.contains('rev_merged_git') ){
			
			if(!(source.endsWith(".java"))){
				temp = sourceDir.getAbsolutePath().replaceFirst('rev_merged_git' , revName)
				//FileUtils.moveFile(sourceDir, new File(temp))
				FileUtils.copyFile(sourceDir, new File(temp))
			}
			
		}else{
			temp = sourceDir.getAbsolutePath().replaceFirst('temp2', revName)
			FileUtils.moveFile(sourceDir, new File(temp))
		}
	}

	public int getFilesEditedByOneDev() {
		return filesEditedByOneDev;
	}

	public int getFilesThatRemainedTheSame() {
		return filesThatRemainedTheSame;
	}

	public void removeNonJavaFiles(File dir){

		File leftFolder = new File (this.revDir + File.separator + this.leftRevName)
		this.auxRemoveNonJavaFiles(leftFolder)

		File baseFolder = new File (this.revDir + File.separator + this.baseRevName)
		this.auxRemoveNonJavaFiles(baseFolder)

		File rightFolder = new File (this.revDir + File.separator + this.rightRevName)
		this.auxRemoveNonJavaFiles(rightFolder)

	}

	private void auxRemoveNonJavaFiles(File dir){
		File[] files = dir.listFiles()

		for(File file : files){

			if(file.isFile()){

				String filePath = file.getAbsolutePath()

				if(!(filePath.endsWith(".java"))){

					if(file.delete()){
						//println(files[i].getName() + " is deleted!");
					}else{
						println(file.getName() + " delete operation has failed.");
					}
				}

			} else if (file.isDirectory()){

				this.auxRemoveNonJavaFiles(file)
			}

		}
	}


	private void auxMoveNonJavaFiles(){

	}

	public static void main(String[] args){
		CompareFiles cp = new CompareFiles("/Users/paolaaccioly/Documents/testeConflictsAnalyzer/testes/rev/rev.revisions")
		cp.ignoreFilesWeDontMerge()
	}
}
