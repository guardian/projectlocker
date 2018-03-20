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
            loading: false,
            error: null
        };

        this.keys = {
            storage: "project_storage_id",
            premiere: "Premiere",   //these are held in metadata elements on the pluto side, and it would be good to sync them too somehow
            prelude: "Prelude",
            aftereffects: "AfterEffects",
            audition: "Audition",
            cubase: "Cubase"
        };

        this.updateDefaultSetting = this.updateDefaultSetting.bind(this);

    }

    componentWillMount(){
        this.refreshData();
    }

    refreshData(){
        this.setState({loading:true}, ()=>{
            Promise.all([axios.get("/api/default"),axios.get("/api/storage"),axios.get("/api/template")]).then(responses=>{
                this.setState({
                    loading: false,
                    error: null,
                    templatesList: responses[2].data.result,
                    storageList: responses[1].data.result,
                    currentValues: responses[0].data.results.reduce((acc,entry)=>{
                        acc[entry.name]=entry.value;
                        return acc;
                    }, {})
                })
            }).catch(error=>this.setState({loading: false, error: error}))
        })
    }

    updateDefaultSetting(newStorageId,keyname){
        axios.put("/api/default/" + keyname, newStorageId, {headers: {'Content-Type': 'text/plain'}}).then(
            window.setTimeout(()=>this.refreshData(),250)
        );
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
        return <div className="mainbody">
            <h3>Server defaults</h3>
            <ErrorViewComponent error={this.state.error}/>
            <table>
                <tbody>
                <tr>
                    <td>Default storage for created projects</td>
                    <td><StorageSelector selectedStorage={this.storagePref()}
                                         selectionUpdated={value=>this.updateDefaultSetting(value, this.keys.storage)}
                                         storageList={this.state.storageList}/></td>
                </tr>
                <tr>
                    <td>Project template to use for Pluto 'Premiere':</td>
                    <td><TemplateSelector selectedTemplate={this.templatePref(this.keys.premiere)}
                                          selectionUpdated={value=>this.updateDefaultSetting(value, this.keys.storage)}
                                          templatesList={this.state.templatesList}/></td>
                </tr>
                <tr>
                    <td>Project template to use for Pluto 'Cubase':</td>
                    <td><TemplateSelector selectedTemplate={this.templatePref(this.keys.cubase)}
                                          selectionUpdated={value=>this.updateDefaultSetting(value, this.keys.cubase)}
                                          templatesList={this.state.templatesList}/></td>
                </tr>
                <tr>
                    <td>Project template to use for Pluto 'Prelude':</td>
                    <td><TemplateSelector selectedTemplate={this.templatePref(this.keys.prelude)}
                                          selectionUpdated={value=>this.updateDefaultSetting(value, this.keys.prelude)}
                                          templatesList={this.state.templatesList}/></td>
                </tr>
                <tr>
                    <td>Project template to use for Pluto 'AfterEffects':</td>
                    <td><TemplateSelector selectedTemplate={this.templatePref(this.keys.aftereffects)}
                                          selectionUpdated={value=>this.updateDefaultSetting(value, this.keys.aftereffects)}
                                          templatesList={this.state.templatesList}/></td>
                </tr>
                <tr>
                    <td>Project template to use for Pluto 'Audition':</td>
                    <td><TemplateSelector selectedTemplate={this.templatePref(this.keys.audition)}
                                          selectionUpdated={value=>this.updateDefaultSetting(value, this.keys.audition)}
                                          templatesList={this.state.templatesList}/></td>
                </tr>

                </tbody>
            </table>
        </div>
    }
}

export default ServerDefaults;