import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';

import StorageTypeComponent from './storage/TypeComponent.jsx';
import StorageLoginComponent from './storage/LoginComponent.jsx';
import StorageSubfolderComponent from './storage/SubfolderComponent.jsx';
import StorageCompletionComponent from './storage/CompletionComponent.jsx';
import CommonMultistepRoot from "./common/CommonMultistepRoot.jsx";

class StorageMultistep extends CommonMultistepRoot {
    constructor(props){
        super(props);

        this.state = {
            strgTypes: [],
            selectedType: 0,
            rootpath: "",
            clientpath: null,
            loginDetails: {}
        }
    }

    getStorageTypeIndex(storageTypeName){
        for(let i=0;i<this.state.strgTypes.length;++i){
            if(this.state.strgTypes[i].name===storageTypeName) return i;
        }
        return 0;
    }

    loadEntity(entryId){
        return axios.get("/api/storage/" + entryId).then(response=>{
            console.log("Got existing entity: ", response);
            this.setState({
                rootpath: response.data.result.rootpath,
                clientpath: response.data.result.clientpath,
                selectedType: this.getStorageTypeIndex(response.data.result.storageType),
                loginDetails: {
                    hostname: response.data.result.host,
                    port: response.data.result.port ? parseInt(response.data.result.port) : null,
                    device: response.data.device,
                    username: response.data.result.user,
                    password: response.data.result.password
                }
            })
        }).catch(error=>{
            console.error(error);
            this.setState({error: error});
        })
    }

    loadDependencies() {
        return axios.get("/api/storage/knowntypes").then((response)=>{
            if(response.data.status!=="ok"){
                console.error(response.data);
            } else {
                this.setState({strgTypes: response.data.types});
            }
        }).catch((error)=>{
            console.error(error);
        })
    }

    render(){
        const steps = [
            {
                name: 'Storage type',
                component: <StorageTypeComponent strgTypes={this.state.strgTypes}
                                                 selectedType={this.state.selectedType}
                                                 currentStorage={this.props.currentEntry}
                                                 valueWasSet={(type)=>this.setState({selectedType: type})}/>
            },
            {
                name: 'Login details',
                component: <StorageLoginComponent currentStorage={this.props.currentEntry}
                                                  strgTypes={this.state.strgTypes}
                                                  selectedType={this.state.selectedType}
                                                  loginDetails={this.state.loginDetails}
                                                  valueWasSet={(loginDetails)=>this.setState({loginDetails: loginDetails})}/>
            },
            {
                name: 'Subfolder location',
                component: <StorageSubfolderComponent currentStorage={this.props.currentEntry}
                                                      rootpath={this.state.rootpath}
                                                      clientpath={this.state.clientpath}
                                                      strgTypes={this.state.strgTypes}
                                                      selectedType={this.state.selectedType}
                                                      valueWasSet={(values) => this.setState({rootpath: values.subfolder, clientpath: values.clientpath})}/>
            },
            {
                name: 'Confirm',
                component: <StorageCompletionComponent currentStorage={this.props.currentEntry}
                                                       strgTypes={this.state.strgTypes}
                                                       selectedType={this.state.selectedType}
                                                       loginDetails={this.state.loginDetails}
                                                       rootpath={this.state.rootpath}
                                                       clientpath={this.state.clientpath}
                                                       currentEntry={this.state.currentEntry}
                />
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default StorageMultistep;
