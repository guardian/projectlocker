import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import UploadingThrobber from '../common/UploadingThrobber.jsx';

class TemplateUploadComponent extends CommonMultistepComponent {
    static propTypes = {
        storages: PropTypes.array.isRequired,
        valueWasSet: PropTypes.func.isRequired
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
            uploading: false
        };

        this.fileChange = this.fileChange.bind(this);
    }

    doUpload(file){
        axios({
            method: "PUT",
            url: "/api/file/" + this.state.fileId + "/content",
            data: file,
            headers: {'Content-Type': 'application/octet-stream'}
        }).then(response=>{
            this.setState({uploading:false, hasFiles: true}, ()=>{
                this.props.valueWasSet(this.state.fileId)
            });
        }).catch(error=>{
            this.setState({uploading:false, error:error});
        })
    }

    createFile(filename){
        return new Promise((resolve,reject)=>{
            this.setState({uploading: true, error: null},()=>{
                const nowtime = new Date().toISOString();

                axios({
                    method: "PUT",
                    url: "/api/file",
                    data: {
                        storage: parseInt(this.state.selectedStorage),
                        filepath: filename,
                        version: 1,
                        user: "",
                        ctime: nowtime,
                        mtime: nowtime,
                        atime: nowtime,
                        hasContent: false,
                        hasLink: false
                    }
                }).then(response=>{
                    this.setState({fileId: response.data.id}, ()=>resolve(response.data));
                }).catch(error=>{
                    this.setState({error: error, uploading: false}, ()=>reject(error));
                })
            });
        });
    }

    fileChange(event){
        /*called when the user changes the file selector*/
        const files = event.target.files || event.dataTransfer.files;
        if(!files.length){
            console.log("No files present");
            return;
        }
        this.createFile(files[0].name).then(()=>
            this.doUpload(files[0])
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

    render(){
        return <div>
            <h3>Template upload</h3>
            <p className="information">Please upload the file that you want to serve as a template</p>
            <table>
                <tbody>
                <tr>
                    <td>Storage</td><td><select onChange={(event)=>this.setState({selectedStorage: event.target.value})}>
                    {
                        this.props.storages.map((strg,index)=><option key={index} value={strg.id}>{this.labelForStorage(strg)}</option>)
                    }</select></td>
                </tr>
                <tr>
                    <td>File to upload</td>
                    <td><input onChange={this.fileChange} type="file" id="template-upload-fileselector"/></td>
                    <td><UploadingThrobber loading={this.state.uploading}/></td>
                </tr>
                </tbody>
            </table>
            <ErrorViewComponent error={this.state.error}/>
        </div>
    }
}

export default TemplateUploadComponent;