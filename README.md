# PayWithTransactpay Android SDK

## Overview

The PayWithTransactpay Android SDK is designed to facilitate the integration of the Transactpay payment gateway into Android applications. This SDK provides an easy way to initiate payment transactions, handle success and failure scenarios, and manage various payment operations.

## Features

- Seamless payment initiation
- Handles success and failure callbacks
- Configurable payment parameters
- Easy integration with existing Android applications

## Installation

### Step 1: Add JitPack Repository

Add the JitPack repository to your project's build.gradle file (usually located at the root level).

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Dependency
Include the PayWithTransactpay library in your app's build.gradle file (usually located in app/build.gradle).
```
dependencies {
    implementation 'com.github.Omamuli-Emmanuel:pay_with_transact_pay:0.0.1'
}
```

## Usage
To use the PayWithTransactpay SDK in your application, follow these steps:

### Step 1: Initialize the Payment Intent
Create an Intent for initiating a payment. This is where you'll specify payment details and callback activities.
```
val intent = PayWithTransactpay.newIntent(
    context = this@MainActivity,
    firstName = "John",
    lastName = "Doe",
    phone = "+1234567890",
    amount = "1000.0", // Amount in your currency
    email = "johndoe@example.com",
    apiKey = "your_api_key_here",
    EncryptionKey = "your_encryption_key_here",
    initiatingActivityClass = MainActivity::class.java, //redirect user when the user cancels the transaction
    successClass = Success::class.java, // redirect user when transaction is successful
    failureClass = Failed::class.java // redirect user when transaction fails for any reason
)
```

### Step 2: Start the Payment Activity
Launch the payment activity using the intent created in the previous step.
```
    startActivity(intent)
```

### Step 3: Implement Callback Activities

Create Success and Failed activities to handle payment results. These activities will be triggered based on the payment outcome.

```class Success : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)
        // Handle successful payment
    }
}
```

```class Failed : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_failed)
        // Handle failed payment
    }
}
```

## Configuration

Ensure that you replace the placeholder values in the PayWithTransactpay.newIntent method with your actual credentials and configuration:

apiKey: Your Transactpay API key.
EncryptionKey: Your encryption key for secure transactions.
baseUrl: The base URL of your Transactpay API.
Example
Here's a complete example of integrating the SDK in your MainActivity.
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Button click listener to initiate payment
        findViewById<Button>(R.id.payButton).setOnClickListener {
            val intent = PayWithTransactpay.newIntent(
                context = this,
                firstName = "John",
                lastName = "Doe",
                phone = "+1234567890",
                amount = "1000.0",
                email = "johndoe@example.com",
                apiKey = "your_api_key_here",
                EncryptionKey = "your_encryption_key_here",
                initiatingActivityClass = MainActivity::class.java,
                successClass = Success::class.java,
                failureClass = Failed::class.java
            )
            startActivity(intent)
        }
    }
}
```
