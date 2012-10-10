#!/usr/bin/groovy

import grisu.control.ServiceInterface
import grisu.frontend.control.login.LoginManager
import grisu.frontend.model.job.JobObject
import grisu.model.FileManager
import grith.jgrith.cred.X509Cred
import grith.jgrith.cred.callbacks.StaticCallback



//@GrabResolver(name='nesi', root='http://code.ceres.auckland.ac.nz/nexus/content/repositories/releases/')
@GrabResolver(name='nesi', root='http://code.ceres.auckland.ac.nz/nexus/content/repositories/public')
@Grab(group='grisu.frontend', module='grisu-client', version='0.5.4')
@Grab(group='org.bouncycastle', module='bcprov-jdk16', version='1.45')
class grisu_probe {

	private JobObject job
	private final ServiceInterface si

	private final String input_file
	private final String output_dir
	private final int waittime

	private final String file_content

	public grisu_probe(ServiceInterface si, String inputFile, String outputDir, int waittime) {
		this.si = si
		this.output_dir = outputDir
		this.input_file = inputFile
		this.waittime = waittime
		file_content = new File(inputFile).getText()
	}

	private void submit() {

		try {

			long start = new Date().getTime()
			job = new JobObject(si)

			job.setTimestampJobname('nagios_test')

			job.setSubmissionLocation('pan:pan.nesi.org.nz')

			job.setApplication('generic')
			job.setCommandline('cat '+FileManager.getFilename(input_file))
			job.setCpus(1)
			job.setWalltimeInSeconds(60)
			job.addInputFileUrl(input_file)

			job.createJob('/nz/nesi')

			add_log 'Submitting job: '+job.getJobname()

			job.submitJob(true)
			long submitted = new Date().getTime()

			add_log 'Waiting for job: '+job.getJobname()
			job.waitForJobToFinish(10)

			long executed = new Date().getTime()

			add_log 'Downloading stdout for job: '+job.getJobname()
			def stdout = job.getStdOutContent()

			if ( stdout != file_content ) {
				throw new RuntimeException("Content mismatch")
			} else {
				add_log "Content matches"
			}

			def jobname = job.getJobname()

			add_log 'Cleaning job: '+job.getJobname()
			job.kill(true)

			long finished = new Date().getTime()

			add_log 'Submission successful'
			
			File success_file = new File(output_dir, "success_"+new Date().getTime())
			success_file.setText(new Date().toString()+': '+jobname+'\t'+'success\n')

		} catch (all) {
			add_log 'Submission failed: '+all.getLocalizedMessage()
			File error_file = new File(output_dir, "error_"+new Date().getTime())
		//	error_file.setText("Job submission failed\n"+Throwables.getStackTraceAsString(all))
			error_file.setText(new Date().toString()+': '+jobname+'\t'+all.getLocalizedMessage()+'\n')
		}
	}
	
	private void add_log(String msg) {
		println msg
	} 
		

	private void startSubmitting() {

		while (true) {
			submit()
//			sleep(waittime*60000)
			sleep(waittime*1000)
		}
	}

	static main(args) {

		def cli = new CliBuilder(usage:'gnagios -c certificate -k key -o output_directory -w waittime')
		cli.with {
			h longOpt: 'help', 'Show usage information'
			o longOpt: 'output', args: 1, argName: 'output', 'path to output directory'
			c longOpt: 'certificate', args: 1, argName: 'certificate', 'path to the x509 certificate'
			k longOpt: 'key', args: 1, argName: 'key', 'path to x509 key'
			w longOpt: 'wait', args:1, argName: 'waittime in minutes', 'time inbetween job submissions'
			i longOpt: 'input_file', args:1, argName: 'input_file', "input text file that gets cat'ed as a job"
		}

		def options = cli.parse(args)

		if (!options) {
			System.err.println('No options specified.')
			System.exit(1)
		}

		if (options.h) {
			cli.usage()
			System.exit(0)
		}

		if (!options.c) {
			System.err.println('No path to certificate specified.')
			cli.usage()
			System.exit(1)
		}


		if (!options.o) {
			System.err.println('No output directory specified.')
			cli.usage()
			System.exit(1)
		} else {
			if (!new File(options.o).exists()) {
				if (!new File(options.o).mkdirs()) {
					System.err.println("Can't create output directory "+options.o+".")
					System.exit(1)
				}
			}
		}

		if (!options.w) {
			System.err.println('No waittime specified.')
			cli.usage()
			System.exit(1)
		}

		if (!options.i){
			System.err.println('No inputfile specified.')
			cli.usage()
			System.exit(1)
		}

		int waittime = Integer.parseInt(options.w)

		StaticCallback pwcb = new StaticCallback("".toCharArray())

		X509Cred c = new X509Cred(pwcb, options.c, options.k)

		def si = LoginManager.login('bestgrid', c, false)

		def probe = new grisu_probe(si, options.i, options.o, waittime)

		probe.startSubmitting()
	}
}
