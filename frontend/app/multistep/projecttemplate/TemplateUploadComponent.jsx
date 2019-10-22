import React from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import axios from 'axios';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import UploadingThrobber from '../common/UploadingThrobber.jsx';
import FileEntryView from '../../EntryViews/FileEntryView.jsx';
import StorageSelector from "../../Selectors/StorageSelector.jsx";
import UploadNewVersionComponent from "./UploadNewVersionComponent.jsx";

class TemplateUploadComponent extends CommonMultistepComponent {
    static propTypes = {
        storages: PropTypes.array.isRequired,
        valueWasSet: PropTypes.func.isRequired,
        existingFileId: PropTypes.number
    };

    constructor(props) {
        super(props);

        const firstStorage = this.props.storages[0] ? this.props.storages[0].id : null;

        this.state = {
            loading: false,
            error: null,
            hasFiles: false,
            selectedStorage: firstStorage,
            fileId: null,
            uploading: false,
            useExisting: true,
            uploadFileVersion: 1,
            uploadFileName: null,
            fileInputKey:0  //we use this to clear the file input component by forcing it to re-render when the value is changed
        };

        this.fileChange = this.fileChange.bind(this);
        this.fileInput = React.createRef();
    }

    componentDidUpdate() {
        //override the super-class implementation as we don't want it here
    }

    UNSAFE_componentWillMount(){
        if(this.props.existingFileId===null) this.setState({useExisting: false});
    }

    doUpload(){
        const data = this.fileInput.current.files[0];
        console.log("doUpload: file name is ", data.name);
        axios({
            method: "PUT",
            url: "/api/file/" + this.state.fileId + "/content",
            data: data,
            headers: {'Content-Type': 'application/octet-stream'}
        }).then(response=>{
            this.setState({uploading:false, hasFiles: true}, ()=>{
                this.props.valueWasSet(this.state.fileId)
            });
        }).catch(error=>{
            this.setState({uploading:false, error:error});
        });
    }

    createFile(){
        return new Promise((resolve,reject)=>{
            if(this.state.uploadFileName===null){
                reject("uploadFileName not set")
            } else {
                this.setState({uploading: true, error: null}, () => {
                    const nowtime = new Date().toISOString();

                    axios({
                        method: "PUT",
                        url: "/api/file",
                        data: {
                            storage: parseInt(this.state.selectedStorage),
                            filepath: this.state.uploadFileName,
                            version: this.state.uploadFileVersion,
                            user: "",
                            ctime: nowtime,
                            mtime: nowtime,
                            atime: nowtime,
                            hasContent: false,
                            hasLink: false
                        }
                    }).then(response => {
                        this.setState({fileId: response.data.id}, () => resolve(response.data));
                    }).catch(error => {
                        console.log(error);
                        console.log(error.response.data);
                        console.log(error.response.data.hasOwnProperty("nextAvailableVersion"));
                        if (error.response.data.hasOwnProperty("nextAvailableVersion")) {
                            console.log("can create object at version ", error.response.data.nextAvailableVersion);
                            this.setState({
                                error: error,
                                uploading: false,
                                uploadFileVersion: error.response.data.nextAvailableVersion
                            }, () => reject(error));
                        } else {
                            this.setState({error: error, uploading: false}, () => reject(error));
                        }
                    })
                });
            }
        });
    }

    fileChange(event){
        /*called when the user changes the file selector*/
        const files = event.target.files || event.dataTransfer.files;
        if(!files.length){
            console.log("No files present");
            return;
        }
        this.setState({uploadFileName: files[0].name}, ()=>
            this.createFile().then(()=>
                this.doUpload()
            )
        );
    }

    labelForStorage(strg){
        const remotePart = strg.hasOwnProperty("user") && strg.hasOwnProperty("host") ? strg.user.toString + "@" + strg.host.toString : "(no credentials)";
        const pathPart = strg.hasOwnProperty("rootpath") ? strg.rootpath.toString() : "/";

        if(strg.storageType==="Local"){
            return strg.storageType + " (" + pathPart + ")"
        } else {
            return strg.storageType + " " + remotePart + " (" + pathPart + ")"
        }
    }

    storageSupportsVersions(){
        const matchingStorageList = this.props.storages.filter((value,idx,arry)=>value===this.state.selectedStorage);
        if(matchingStorageList.length===0) return false;
        console.log("storageSupportsVersions: ", matchingStorageList[0].supportsVersions);
    }
    render(){
        return <div>
            <h3>Template upload</h3>
            <p className="information">Please upload the file that you want to serve as a template</p>
            <input type="radio" name="uploadOrUseExisting"
                   id="useExisting" checked={this.state.useExisting} disabled={this.props.existingFileId===null}
                   onChange={event=>this.setState({useExisting: !this.state.useExisting})}/>
                    <span style={{marginLeft: "1em"}}>Use existing template file:
                        <FileEntryView entryId={this.props.existingFileId} hide={!this.props.existingFileId}/>
                    </span><br/>
            <input type="radio" name="uploadOrUseExisting"
                   id="uploadNew" checked={!this.state.useExisting}
                   onChange={event=>this.setState({useExisting: !this.state.useExisting})}/>
            <span style={{marginLeft: "1em"}}>Upload new file</span>

            <table style={{display: this.state.useExisting ? "none" : "inherit"}}>
                <tbody>
                <tr>
                    <td>Storage</td>
                    <td><StorageSelector selectedStorage={this.state.selectedStorage}
                                         selectionUpdated={newValue=>this.setState({selectedStorage: newValue})}
                                         storageList={this.props.storages}
                                         showLabel={true}
                                         enabled={true}/>
                    </td>
                </tr>
                <tr>
                    <td>File to upload</td>
                    <td><input onChange={this.fileChange} type="file" id="template-upload-fileselector" key={this.state.fileInputKey} ref={this.fileInput}/></td>
                    <td><UploadingThrobber loading={this.state.uploading}/>{this.state.hasFiles? <p style={{color:"green"}}>File uploaded to version {this.state.uploadFileVersion}, please click "Next"</p> : <p/>}</td>
                </tr>
                </tbody>
            </table>
            <ErrorViewComponent error={this.state.error}/>
            { (this.state.uploadFileVersion===1 || this.state.hasFiles) ? <p/> : <UploadNewVersionComponent
                newVersionCb={()=>this.createFile().then(()=>this.doUpload())}
                cancelCb={()=>this.setState({fileInputKey: this.state.fileInputKey+1, uploadFileVersion: 1, error:null})}/>
            }

        </div>
    }
}

export default TemplateUploadComponent;