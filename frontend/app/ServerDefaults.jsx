import React from 'react';
import axios from 'axios';
import StorageSelector from './Selectors/StorageSelector.jsx';
import ErrorViewComponent from "./multistep/common/ErrorViewComponent.jsx";
import TemplateSelector from "./Selectors/TemplateSelector.jsx";

class ServerDefaults extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            currentValues: {},
            storageList: [],
            templatesList: [],
            plutoProjectTypes: [],
            loading: false,
            error: null
        };

        this.keys = {
            storage: "project_storage_id"
        };

        this.updateDefaultSetting = this.updateDefaultSetting.bind(this);

    }

    componentWillMount(){
        this.refreshData();
    }

    refreshData(){
        this.setState({loading:true}, ()=>{
            Promise.all([axios.get("/api/default"),axios.get("/api/storage"),axios.get("/api/template"),axios.get("/api/plutoprojecttypeid")]).then(responses=>{
                this.setState({
                    loading: false,
                    error: null,
                    templatesList: responses[2].data.result,
                    storageList: responses[1].data.result,
                    currentValues: responses[0].data.results.reduce((acc,entry)=>{
                        acc[entry.name]=entry.value;
                        return acc;
                    }, {}),
                    plutoProjectTypes: responses[3].data.result
                })
            }).catch(error=>this.setState({loading: false, error: error}))
        })
    }

    updateDefaultSetting(newStorageId,keyname){
        axios.put("/api/default/" + keyname, newStorageId, {headers: {'Content-Type': 'text/plain'}}).then(
            window.setTimeout(()=>this.refreshData(),250)
        );
    }

    updateProjectTemplateSetting(newValue, plutoProjectTypeEntryId){
        if(newValue==="-1"){
            axios.delete("/api/plutoprojecttypeid/" + plutoProjectTypeEntryId + "/default-template").then(
                window.setTimeout(() => this.refreshData(), 450)
            );
        } else {
            axios.put("/api/plutoprojecttypeid/" + plutoProjectTypeEntryId + "/default-template/" + newValue).then(
                window.setTimeout(() => this.refreshData(), 450)
            );
        }
    }

    /* return the current default storage, or first in the list, or zero if neither is present */
    storagePref(){
        if(this.state.currentValues.hasOwnProperty(this.keys.storage)){
            return this.state.currentValues[this.keys.storage];
        } else {
            if(this.state.storageList.length>0)
                return this.state.storageList[0].id;
            else
                return 0;
        }
    }

    /* return the current default template, or first in the list, or zero if neither is present */
    templatePref(keyname){
        if(this.state.currentValues.hasOwnProperty(keyname)){
            return this.state.currentValues[keyname];
        } else {
            if(this.state.templatesList.length>0)
                return this.state.templatesList[0].id;
            else
                return 0;
        }
    }

    render(){
        const plutoProjectTypeList = Object.assign([], this.state.plutoProjectTypes).sort((a,b)=>{
            if(a.name < b.name) return -1;
            if(a.name > b.name) return 1;
            return 0;
        });

        return <div className="mainbody">
            <h3>Server defaults</h3>
            <ErrorViewComponent error={this.state.error}/>
            <table>
                <tbody>
                <tr>
                    <td>Default storage for created projects</td>
                    <td><StorageSelector enabled={true}
                                         selectedStorage={this.storagePref()}
                                         selectionUpdated={value=>this.updateDefaultSetting(value, this.keys.storage)}
                                         storageList={this.state.storageList}/></td>
                </tr>
                {
                    plutoProjectTypeList.map(projectTypeEntry=><tr>
                        <td>Project template to use for Pluto '{projectTypeEntry.name}':</td>
                        <td><TemplateSelector allowNull={true}
                                              selectedTemplate={projectTypeEntry["defaultProjectType"]}
                                              selectionUpdated={value=>this.updateProjectTemplateSetting(value, projectTypeEntry.id)}
                                              templatesList={this.state.templatesList}
                        /></td>
                    </tr>)
                }
                </tbody>
            </table>
        </div>
    }
}

export default ServerDefaults;