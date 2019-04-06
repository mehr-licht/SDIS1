package service;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The Remote interface serves to identify interfaces whose methods may be invoked from a non-local virtual machine.
 * Any object that is a remote object must directly or indirectly implement this interface.
 * Only those methods specified in a "remote interface", an interface that extends java.rmi.Remote
 * are available remotely
 * */
public interface My_Interface_Remote extends Remote {

  /**
   * @param pathname path do ficheiro
   * @param replicationDegree  grau de replicação
   * */
  void backup(String pathname, int replicationDegree) throws RemoteException;

  /**
   * @param pathname path do ficheiro
   * */
  void restore(String pathname) throws RemoteException;

  /**
   * @param pathname path do ficheiro
   * */
  void delete(String pathname) throws RemoteException;

  /**
   * Space numeber in Kbytes
   * */
  void reclaim(int space) throws RemoteException;


  void state() throws RemoteException;

}
