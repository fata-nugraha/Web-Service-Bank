/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webedeh.bank;

import java.sql.*;
import java.util.ArrayList;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import com.webedeh.bank.Transaksi;
import com.webedeh.bank.Nasabah;
import java.util.Random;

/**
 *
 * @author FtN
 */
@WebService(serviceName = "bankws")
public class bankws {
    SQLConn connectSQL = new SQLConn();
    Connection conn = null;
    PreparedStatement pstm = null;
 
    @WebMethod(operationName = "gettransaksi")
    public ArrayList <Transaksi> getTransaksi() {
        ArrayList <Transaksi> transaksi = new ArrayList <>();
        try {
            ResultSet rs = connectSQL.query("SELECT * FROM transaksi");
            while (rs.next()) {
                Transaksi trans = new Transaksi();
                trans.nomorRekening = rs.getInt("nomor_rekening");
                trans.jenisTransaksi = rs.getString("jenis_transaksi");
                trans.jumlah = rs.getInt("jumlah");
                trans.rekeningTerkait = rs.getInt("rekening_terkait");
                trans.waktuTransaksi = rs.getTimestamp("waktu_transaksi").toString();
                transaksi.add(trans);
            }
        } catch (Exception e) {
            Transaksi trans = new Transaksi();
            trans.nomorRekening = -1;
            trans.jenisTransaksi = e.toString();
            transaksi.add(trans);
        }
        return transaksi;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "generateAkunVirtual")
    public Integer generateAkunVirtual(@WebParam(name = "rekening") int rekening) {
        Random randomGenerator = new Random();
        Boolean available = false;
        Boolean result = false;
        int randomInt = 0;
        try {
            while (!(available)){
                randomInt = randomGenerator.nextInt(999) + 16517000;
                ResultSet rs = connectSQL.query("SELECT IF( EXISTS( SELECT * FROM akun_virtual WHERE (nomor_virtual) = " + randomInt + "), 1, 0) as exist");
                if (rs.next()) {
                    if (rs.getInt("exist")==0){
                        available = true;
                    }
                }
            }
            
            result = connectSQL.update("INSERT INTO akun_virtual(nomor_rekening, nomor_virtual) VALUES (" + rekening + ", " + randomInt + ")");
            if (result){
                return randomInt;
            }
            
        } catch (Exception e) {
            return -1;
        }
        return randomInt;
    }
    
    /**
     * Web service operation
     */
    @WebMethod(operationName = "getDataNasabah")
    public Nasabah getDataNasabah(@WebParam(name = "rekening") int rekening) {
        Nasabah nasabah = new Nasabah();
        try {
            ResultSet rs_transaksi = connectSQL.query("SELECT * FROM transaksi WHERE nomor_rekening = " + rekening);
            ResultSet rs_pemilik = connectSQL.query("SELECT * FROM rekening WHERE nomor_rekening = " + rekening);
            if (rs_pemilik.next()) {
                nasabah.no_rekening = rs_pemilik.getInt("nomor_rekening");
                nasabah.nama = rs_pemilik.getString("nama_pemilik");
                nasabah.saldo = rs_pemilik.getInt("saldo");
            }
            nasabah.list_transaksi = new ArrayList<> ();
            while (rs_transaksi.next()) {
                Transaksi trans = new Transaksi();
                trans.nomorRekening = rs_transaksi.getInt("nomor_rekening");
                trans.jenisTransaksi = rs_transaksi.getString("jenis_transaksi");
                trans.jumlah = rs_transaksi.getInt("jumlah");
                trans.rekeningTerkait = rs_transaksi.getInt("rekening_terkait");
                trans.waktuTransaksi = rs_transaksi.getTimestamp("waktu_transaksi").toString();
                nasabah.list_transaksi.add(trans);
            }
        } catch (Exception e) {
            Transaksi trans = new Transaksi();
            nasabah.no_rekening = -1;
            nasabah.nama = e.toString();
        }
        return nasabah;
    }
    
    @WebMethod(operationName = "transfer")
    public Boolean transfer(@WebParam(name = "rekening") int rekening, @WebParam(name = "rekening_tujuan") int rekening_tujuan, @WebParam(name = "uang") int uang) {
        Boolean result1 = false;
        Boolean result2 = false;
        Boolean result3 = false;
        Boolean result4 = false;
        String success = "success";
        try {
            //Check transfer preconditions
            ResultSet rs_rekening = connectSQL.query("SELECT IF( EXISTS( SELECT * FROM rekening WHERE (nomor_rekening) = " + rekening_tujuan + "), 1, 0) as exist");
            if (rs_rekening.next()) {
                if (rs_rekening.getInt("exist")==0){
                    ResultSet rs_virtual = connectSQL.query("SELECT IF( EXISTS( SELECT * FROM akun_virtual WHERE (nomor_virtual) = " + rekening_tujuan + "), (SELECT nomor_rekening FROM akun_virtual WHERE nomor_virtual = " + rekening_tujuan + "), 0) as no_rekening");
                    if(rs_virtual.next()) {
                        if (rs_virtual.getInt("no_rekening")==0){
                            return false;
                        }else{
                            rekening_tujuan = rs_virtual.getInt("no_rekening");
                        }
                    }
                }
            }
            ResultSet rs_saldo = connectSQL.query("SELECT saldo FROM rekening WHERE nomor_rekening = " + rekening);
            if (rs_saldo.next()){
                if (rs_saldo.getInt("saldo")<uang){
                    return false;
                }
            }

            
            //Transfer
            result1 = connectSQL.update("INSERT INTO transaksi(nomor_rekening, jenis_transaksi, jumlah, rekening_terkait) "
                    + "VALUE (" + rekening + ", " + "'debit'," + uang + "," + rekening_tujuan + ")");
            result2 = connectSQL.update("UPDATE rekening SET saldo = saldo - " + uang + " WHERE nomor_rekening = " + rekening);
            result3 = connectSQL.update("INSERT INTO transaksi(nomor_rekening, jenis_transaksi, jumlah, rekening_terkait) "
                    + "VALUE (" + rekening_tujuan + ", " + "'kredit'," + uang + "," + rekening + ")");
            result4 = connectSQL.update("UPDATE rekening SET saldo = saldo + " + uang + " WHERE nomor_rekening = " + rekening_tujuan);
            
        } catch (Exception e) {
            return false;
        }
        return result1&&result2;
    }
    
    /**
     * Web service operation
     */
    @WebMethod(operationName = "isRekeningValid")
    public Boolean isRekeningValid(@WebParam(name = "rekening") int rekening) {
        boolean rekening_valid = false;
        try {
            ResultSet rs = connectSQL.query("SELECT IF( EXISTS( SELECT * FROM rekening WHERE (nomor_rekening) = " + rekening + "), 1, 0) as exist");
            if (rs.next()) {
                if (rs.getInt("exist")==1){
                    rekening_valid = true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return rekening_valid;
    }
    
    @WebMethod(operationName = "cekTransasksi")
    public Boolean cekTransasksi(@WebParam(name = "rekening_virtual_tujuan") int rekening_virtual_tujuan, @WebParam(name = "nominal") int nominal, @WebParam(name = "start_datetime") String start_datetime, @WebParam(name = "end_datetime") String end_datetime) {
        boolean available = true;
        try {
            ResultSet rs = connectSQL.query("SELECT IF( EXISTS( SELECT * FROM transaksi JOIN akun_virtual ON (rekening_terkait = akun_virtual.nomor_rekening)"
            + " WHERE (rekening_terkait = " + rekening_virtual_tujuan +" OR nomor_virtual = " + rekening_virtual_tujuan
            + ") AND jenis_transaksi='kredit' AND jumlah = "+ nominal 
            + " AND waktu_transaksi>= "+ start_datetime +" AND waktu_transaksi<= "+ end_datetime +" ), 1, 0) as exist");
            if (rs.next()) {
                if (rs.getInt("exist")==0){
                    available = false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return available;
    }
}
