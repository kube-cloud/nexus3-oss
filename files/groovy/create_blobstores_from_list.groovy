import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.common.collect.NestedAttributesMap

// Define GCS Blobstore Type
String GCS_BLOBSTORE_TYPE = "Google Cloud Storage";

// Define Configu Key
String CONFIG_KEY = GCS_BLOBSTORE_TYPE.toLowerCase()

// Define GCS Blobstore Bucket Name Key
String BUCKET_NAME_KEY = "bucketName"

// Define GCS Blobstore Bucket Credentials File Key
String CREDENTIAL_FILE_PATH_KEY = "credentialFilePath"

// Define GCS Blobstore Bucket Region Key
String REGION_KEY = "region"

// Parse Arguments (BlobStore Configuration Array) to Object
parsed_args = new JsonSlurper().parseText(args)

// Initialize Action Details
List<Map<String, String>> actionDetails = []

// Initialize Script Results
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

parsed_args.each { blobstoreDef ->
    
    Map<String, String> currentResult = [name: blobstoreDef.name, type: blobstoreDef.get('type', 'file')]
    
    existingBlobStore = blobStore.getBlobStoreManager().get(blobstoreDef.name)
    if (existingBlobStore == null) {
        try {
            if (blobstoreDef.type == "S3") {
                blobStore.createS3BlobStore(blobstoreDef.name, blobstoreDef.config)
                msg = "S3 blobstore {} created"
            } else if (blobstoreDef.type == "GCS") {

                // Log
                log.info("Create GCS Blobstore : {}", blobstoreDef)

                // Instantiate a new BlobStore Configuration
                gcpBlobStore = blobStore.getBlobStoreManager().newConfiguration()

                // Initialize Type
                gcpBlobStore.setType(GCS_BLOBSTORE_TYPE);

                // Initialize Name
                gcpBlobStore.setName(blobstoreDef.name);

                // Get Nested Attribute MAP for the Custom GCS Config
                nestedAttributesMap = gcpBlobStore.attributes(CONFIG_KEY)

                // Initialize Custom GCS Properties
                nestedAttributesMap.set(BUCKET_NAME_KEY, blobstoreDef.config.bucketName)
                nestedAttributesMap.set(REGION_KEY, blobstoreDef.config.bucketRegion)
                nestedAttributesMap.set(CREDENTIAL_FILE_PATH_KEY, blobstoreDef.config.credentialFilePath)

                // Log
                log.info("Ready to Create GCS Blobstore : {}, GCS Path : {}", gcpBlobStore)

                // Create BlobStore
                blobStore.getBlobStoreManager().create(gcpBlobStore)

                // Processing Message
                msg = "GCS Blobstore {} Created"

            } else {
                blobStore.createFileBlobStore(blobstoreDef.name, blobstoreDef.path)
                msg = "File blobstore {} created"
            }
            log.info(msg, blobstoreDef.name)
            currentResult.put('status', 'created')
            scriptResults['changed'] = true
        } catch (Exception e) {
            log.error('Could not create blobstore {}: {}', blobstoreDef.name, e.toString())
            currentResult.put('status', 'error')
            scriptResults['error'] = true
            currentResult.put('error_msg', e.toString())
        }
    } else {
        log.info("Blobstore {} already exists. Left untouched", blobstoreDef.name)
        currentResult.put('status', 'exists')
    }

    scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)
