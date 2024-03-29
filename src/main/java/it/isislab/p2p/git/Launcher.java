package it.isislab.p2p.git;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import it.isislab.p2p.git.entity.Commit;
import it.isislab.p2p.git.entity.Item;
import it.isislab.p2p.git.exceptions.ConflictsNotResolvedException;
import it.isislab.p2p.git.exceptions.GeneratedConflictException;
import it.isislab.p2p.git.exceptions.NothingToPushException;
import it.isislab.p2p.git.exceptions.RepoStateChangedException;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.exceptions.RepositoryNotExistException;
import it.isislab.p2p.git.implementations.TempestGit;

public class Launcher {

	@Option(name = "-m", aliases = "--masterip", usage = "IP del master peer", required = true)
	private static String master;

	@Option(name = "-id", aliases = "--identifierpeer", usage = "L'identificativo univoco del peer", required = true)
	private static int id;

	@Option(name = "-wd", aliases = "--workdirectory", usage = "Path in cui verranno conservate le repository", required = true)
	private static String work_dir;

	private static void printMenu() {
		System.out.println("🍳 Menu: ");
		System.out.println(" 1 - Crea una Repository");
		System.out.println(" 2 - Clona una Repository");
		System.out.println(" 3 - Aggiungi file a una repository");
		System.out.println(" 4 - Commit");
		System.out.println(" 5 - Push");
		System.out.println(" 6 - Pull");
		System.out.println(" 8 - Mostra lo stato di una repository remota");
		System.out.println(" 9 - Mostra lo stato di una repository locale");
		System.out.println("10 - Mostra commits locali in coda");
		System.out.println("11 - Elimina le repository ed Esci 🚪");
		System.out.println();
	}

	public static void main(String[] args) {
		final CmdLineParser parser = new CmdLineParser(new Launcher());

		try {
			parser.parseArgument(args);
			TextIO textIO = TextIoFactory.getTextIO();
			TempestGit peer = new TempestGit(id, master, Path.of(work_dir));

			System.out.println("\nPeer: " + id + " on Master: " + master + " Word dir: " + work_dir + "\n");

			boolean flag = true;
			while (flag) {
				printMenu();
				int option = textIO.newIntInputReader().withMaxVal(11).withMinVal(1).read("Scelta");

				String repo_name;

				switch (option) {
				case 1:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					String dir_init = textIO.newStringInputReader().withDefaultValue("src/test/resources/start_files").read("Directory di inizializzazione:");
					String dest_dir = textIO.newStringInputReader().withDefaultValue(repo_name).read("Directory di destinazione:");

					try {
						if (peer.createRepository(repo_name, Paths.get(dir_init), Paths.get(dest_dir))) {
							System.out.println(peer.get_remote_repo(repo_name).toString());
							System.out.println("\nRepository \"" + repo_name + "\" creata con successo ✅\n");
						} else
							System.out.println("\nErrore nella creazione della repository ❌\n");
					} catch (RepositoryAlreadyExistException e) {
						System.out.println("\nLa repository \"" + repo_name + "\" esiste già ❌\n");
					}

					break;

				case 2:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					String dir_clone = textIO.newStringInputReader().withDefaultValue(repo_name).read("Directory di destinazione:");

					try {
						if (peer.clone(repo_name, Paths.get(dir_clone)))
							System.out.println("\nRepository \"" + repo_name + "\" clonata correttamente  ✅\n");
						else
							System.out.println("\nErrore nel clonare la repository ❌\n");
					} catch (RepositoryNotExistException e) {
						System.out.println("\nLa repository \"" + repo_name + "\" non esiste ❌\n");
					}
					break;

				case 3:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					String add_dir = textIO.newStringInputReader().withDefaultValue("src/test/resources/add_files").read("Directory da aggiungere:");

					try {
						Collection<Item> file_added = peer.addFilesToRepository(repo_name, Paths.get(add_dir));

						if (file_added != null) {
							System.out.println("\nOperazione andata a buon fine ✅");
							System.out.println("--------------------------------------------------------------------------------");
							System.out.println("Sono stati aggiunti " + file_added.size() + " file: ");
							for (Item item : file_added) {
								System.out.println("\t🔸 " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + " bytes");
							}
							System.out.println("--------------------------------------------------------------------------------");

						} else
							System.out.println("\nErrore nell'aggiunta dei file, controllare la directory ❌\n");
					} catch (RepositoryNotExistException e) {
						System.out.println("\nLa repository \"" + repo_name + "\" non esiste ❌\n");
					}

					break;

				case 4:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					String message = textIO.newStringInputReader().withDefaultValue("Ho cambiato qualcosa 🤷").read("Messaggio di commit:");

					Commit last_commit = peer.commit(repo_name, message);

					if (last_commit != null) {
						System.out.println("\nIl seguente commit è stato generato:");
						System.out.println(last_commit.toString());
						System.out.println("\nCommit sulla repository \"" + repo_name + "\" creato correttamente ✅\n");
					} else
						System.out.println("\nNessuna modifica trovata ❌\n");
					break;

				case 5:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");

					if (peer.get_local_commits(repo_name) != null) {
						System.out.println("\nI seguenti commit saranno elaborati: ");
						for (Commit commit : peer.get_local_commits(repo_name)) {
							System.out.println(commit.toString());
						}
					}

					try {
						if (peer.push(repo_name))
							System.out.println("\nPush sulla repository \"" + repo_name + "\" creato correttamente ✅\n");
					} catch (RepoStateChangedException e) {
						System.out.println("\n⚠️ Stato della repository remota cambiato, necessario pull\n");
					} catch (NothingToPushException e) {
						System.out.println("\n⚠️ Nessun commit in coda\n");
					} catch (RepositoryNotExistException e) {
						System.out.println("\nLa repository inserita non esiste ❌\n");
					}
					break;

				case 6:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					try {
						if (peer.pull(repo_name)) {
							System.out.println("\nPull della repository \"" + repo_name + "\" creato correttamente ✅\n");
						} else {
							System.out.println("\nErrore nalla fase di pull ❌\n");
						}
					} catch (RepositoryNotExistException e) {
						System.out.println("\nLa repository inserita non esiste ❌\n");
					} catch (GeneratedConflictException e) {
						System.out.println("\n⚠️ È stato generato un conflitto risolverlo prima di continuare\n");
					} catch (ConflictsNotResolvedException e) {
						System.out.println("\n⚠️ Risolvere prima tutti i conflitti per continuare (Rimuovere etichette REMOTE e LOCAL)\n");
					}
					break;

				case 7:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					if (peer.removeRepo(repo_name))
						System.out.println("\nRepository \"" + repo_name + "\" correttamente eliminata ✅\n");
					else
						System.out.println("\nErrore nell'eliminazione della repository ❌\n");
					break;

				case 8:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					System.out.println(peer.get_remote_repo(repo_name).toString());
					break;

				case 9:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					System.out.println(peer.get_local_repo(repo_name).toString());
					break;

				case 10:
					repo_name = textIO.newStringInputReader().withDefaultValue("Repo_test").read("Nome Repo:");
					if (peer.get_local_commits(repo_name) != null)
						for (Commit commit : peer.get_local_commits(repo_name)) {
							System.out.println(commit.toString());
						}
					else {
						System.out.println("\n⚠️  Nessun commit in coda. \n");
					}
					break;

				case 11:
					if (peer.leaveNetwork()) {
						System.out.println("\nDisconnessione completata ✅");
						flag = false;
					} else
						System.out.println("\nErrore nella disconnessione ❌");
					break;

				default:
					break;
				}
			}
		} catch (CmdLineException clEx) {
			System.err.println("ERRORE: Impossibile completare il parsing delle opzioni: " + clEx);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}