package com.example.mongo_migrate_multids.helpers;

import org.bson.types.ObjectId;

import java.math.BigInteger;

public class ObjectIdHelpers {

    public static String convertObjectIdToLargeInteger(String objectIdString) {
        if (objectIdString == null) {
            throw new IllegalArgumentException("ObjectId cannot be null");
        }
        ObjectId objectId = new ObjectId(objectIdString);
        // Get the byte array representation of the ObjectId
        byte[] objectIdBytes = objectId.toByteArray();

        // Convert byte array to an unsigned BigInteger
        BigInteger bigInteger = new BigInteger(1, objectIdBytes);

        // Return the BigInteger as a string
        return bigInteger.toString();
    }

    public static ObjectId convertLargeIntegerToObjectId(String largeIntegerString) {
        if (largeIntegerString == null) {
            throw new IllegalArgumentException("Large integer string cannot be null");
        }
        BigInteger bigInteger = new BigInteger(largeIntegerString);
        byte[] objectIdBytes = bigInteger.toByteArray();

        // Ensure the byte array is 12 bytes long
        if (objectIdBytes.length != 12) {
            byte[] temp = new byte[12];
            if (objectIdBytes.length > 12) {
                System.arraycopy(objectIdBytes, objectIdBytes.length - 12, temp, 0, 12);
            } else {
                System.arraycopy(objectIdBytes, 0, temp, 12 - objectIdBytes.length, objectIdBytes.length);
            }
            objectIdBytes = temp;
        }

        return new ObjectId(objectIdBytes);
    }

    public static String convertLargeIntegerToStringId(String largeIntegerString) {
        if (largeIntegerString == null) {
            throw new IllegalArgumentException("Large integer string cannot be null");
        }
        BigInteger bigInteger = new BigInteger(largeIntegerString);
        byte[] objectIdBytes = bigInteger.toByteArray();

        // Ensure the byte array is 12 bytes long
        if (objectIdBytes.length != 12) {
            byte[] temp = new byte[12];
            if (objectIdBytes.length > 12) {
                System.arraycopy(objectIdBytes, objectIdBytes.length - 12, temp, 0, 12);
            } else {
                System.arraycopy(objectIdBytes, 0, temp, 12 - objectIdBytes.length, objectIdBytes.length);
            }
            objectIdBytes = temp;
        }

        return new ObjectId(objectIdBytes).toHexString();
    }

    public static void main(String[] args) {
        // Convert ObjectId to large integer
        String largeInteger = convertObjectIdToLargeInteger("5bddd18e9dc6d630ca0d5157");

        System.out.println("Converted Large Integer: " + largeInteger);
    }
}
