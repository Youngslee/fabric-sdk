package org.hyperledger.fabric_ca.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Test {

	private static HFClient client = null;

	public static void main(String[] args) throws Throwable {
		changeFileNames("c:\\tmp\\creds");
		changeFileContents("c:\\tmp\\creds");
		    	/*
		         * wallet_path: path.join(__dirname, './creds'), user_id: 'PeerAdmin',
		         * channel_id: 'mychannel', chaincode_id: 'fabcar', network_url:
		         * 'grpc://192.168.99.100:7051', orderer: grpc://192.168.99.100:7050
		         * 
		         */
		    	
		        // just new objects, without any payload inside
		        client = HFClient.createNewInstance();
		        CryptoSuite cs = CryptoSuite.Factory.getCryptoSuite();
		        client.setCryptoSuite(cs);
		
		        // We implement User interface below in code
		        // folder c:\tmp\creds should contain PeerAdmin.cert (extracted from HF's fabcar
		        // example's PeerAdmin json file)
		        // and PeerAdmin.priv (copy from
		        // cd96d5260ad4757551ed4a5a991e62130f8008a0bf996e4e4b84cd097a747fec-priv)
		        
		        User user = new SampleUser_New("c:\\tmp\\creds", "PeerAdmin");
		        // "Log in"
		        client.setUserContext(user);
		
		        // Instantiate channel
		        Channel channel = client.newChannel("mychannel");
		        channel.addPeer(client.newPeer("peer0.org1.example.com", "grpc://10.70.27.140:7051"));
		        // It always wants orderer, otherwise even query does not work
		        channel.addOrderer(client.newOrderer("orderer.example.com", "grpc://10.70.27.140:7050"));
		        channel.initialize();
		
		        // below is querying and setting new owner
		
		        String newOwner = "New Owner #" + new Random(new Date().getTime()).nextInt(999);
		        System.out.println("New owner is '" + newOwner + "'\n");
		        String[] s = new String[]{"CAR12", "Chevy", "Volt", "Red", "Nick"};
		        queryCreateFabcar(channel, s, true);
		//        queryAllCarFabcar(channel, "");
		//        updateCarOwner(channel, "CAR1", newOwner, false);
		//
		        System.out.println("after request for transaction without commit");
		        //queryFabcar(channel, "CAR1");
		//        queryCreateFabcar(channel, s, true);
		       // updateCarOwner(channel, "CAR1", newOwner, true);
		
		        System.out.println("after request for transaction WITH commit");
		        queryAllCarFabcar(channel, "");
		        Thread.sleep(5000); // 5secs
		        queryAllCarFabcar(channel, "");
		        //queryFabcar(channel, "CAR1");
		//
		//        System.out.println("Sleeping 5s");
		//        Thread.sleep(5000); // 5secs
		//        queryFabcar(channel, "CAR1");
		//        System.out.println("all done");
	}
	public static void changeContents(File file)  {

		JSONParser parser = new JSONParser();
		Object obj;
		FileWriter writer = null;
		try {
			obj = parser.parse(new FileReader(file));
			JSONObject jsonObj = (JSONObject) obj;

			JSONObject enrollmentObj =  (JSONObject) jsonObj.get("enrollment");
			JSONObject identityObj =  (JSONObject) enrollmentObj.get("identity");
			String certificate =  (String) identityObj.get("certificate");
			writer = new FileWriter(file);
			writer.write(certificate);
			System.out.println(file);
			writer.flush();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	public static void renameFile(String filename, String newFilename) {
		File file = new File( filename );
		File fileNew = new File( newFilename );
		if( file.exists() ) {
			file.renameTo( fileNew );
		}
	}

	synchronized public static void changeFileNames(String source) throws ParseException, IOException{
		File dir = new File(source); 
		File[] fileList = dir.listFiles(); 
		for(int i = 0 ; i < fileList.length ; i++){
			File file = fileList[i];
			if(file.getName().contains("user")) {
				renameFile(file.getPath(), source+"\\PeerAdmin.cert");
			}
			else if(file.getName().contains("-priv")) {
				renameFile(file.getPath(), source+"\\PeerAdmin.priv");
			}
		}
		
	}
	synchronized public static void changeFileContents(String source) throws ParseException, IOException{
		File dir = new File(source); 
		File[] fileList = dir.listFiles(); 
		for(int i = 0; i< fileList.length; i++) {
			File file = fileList[i];
			if(file.getName().contains(".cert")) {
				changeContents(file);
			}
		}
	}
	private static void queryCreateFabcar(Channel channel, String[] key, Boolean doCommit) throws Exception {


		TransactionProposalRequest req = client.newTransactionProposalRequest();
		ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
		req.setChaincodeID(cid);
		req.setFcn("createCar");
		req.setArgs( key );
		System.out.println("Executing for " + key);
		Collection<ProposalResponse> resps = channel.sendTransactionProposal(req);
		if (doCommit) {
			channel.sendTransaction(resps);
		}

	}
	private static void queryAllCarFabcar(Channel channel, String key) throws Exception {
		QueryByChaincodeRequest req = client.newQueryProposalRequest();
		ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
		req.setChaincodeID(cid);
		req.setFcn("queryAllCars");
		req.setArgs(new String[] {key});
		System.out.println("Querying for " + key);
		Collection<ProposalResponse> resps = channel.queryByChaincode(req);
		for (ProposalResponse resp : resps) {
			String payload = new String(resp.getChaincodeActionResponsePayload());
			System.out.println("response: " + payload);
		}

	}
	private static void updateCarOwner(Channel channel, String key, String newOwner, Boolean doCommit)
			throws Exception {
		TransactionProposalRequest req = client.newTransactionProposalRequest();
		ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
		req.setChaincodeID(cid);
		req.setFcn("changeCarOwner");
		req.setArgs(new String[] { key, newOwner });
		System.out.println("Executing for " + key);
		Collection<ProposalResponse> resps = channel.sendTransactionProposal(req);
		if (doCommit) {
			channel.sendTransaction(resps);
		}
	}

}

/***
 * Implementation of user. main business logic (as for fabcar example) is in
 * getEnrollment - get user's private key and cert
 * 
 */
class SampleUser_New implements User {
	private final String certFolder;
	private final String userName;

	public SampleUser_New(String certFolder, String userName) {
		this.certFolder = certFolder;
		this.userName = userName;
	}

	@Override
	public String getName() {
		return userName;
	}

	@Override
	public Set<String> getRoles() {
		return new HashSet<String>();
	}

	@Override
	public String getAccount() {
		return "";
	}

	@Override
	public String getAffiliation() {
		return "";
	}

	@Override
	public Enrollment getEnrollment() {
		return new Enrollment() {

			@Override
			public PrivateKey getKey() {
				try {
					return loadPrivateKey(Paths.get(certFolder, userName + ".priv"));
				} catch (Exception e) {
					return null;
				}
			}

			@Override
			public String getCert() {
				try {
					return new String(Files.readAllBytes(Paths.get(certFolder, userName + ".cert")));
				} catch (Exception e) {
					return "";
				}
			}

		};
	}

	@Override
	public String getMspId() {
		return "Org1MSP";
	}
	/***
	 * loading private key from .pem-formatted file, ECDSA algorithm
	 * (from some example on StackOverflow, slightly changed)
	 * @param fileName - file with the key
	 * @return Private Key usable
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public static PrivateKey loadPrivateKey(Path fileName) throws IOException, GeneralSecurityException {
		PrivateKey key = null;
		InputStream is = null;
		try {
			is = new FileInputStream(fileName.toString());
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder builder = new StringBuilder();
			boolean inKey = false;
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if (!inKey) {
					if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
						inKey = true;
					}
					continue;
				} else {
					if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
						inKey = false;
						break;
					}
					builder.append(line);
				}
			}
			//
			byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
			KeyFactory kf = KeyFactory.getInstance("EC");
			key = kf.generatePrivate(keySpec);
		} finally {
			is.close();
		}
		return key;
	}
}
