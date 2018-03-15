import React from 'react';
import axios from 'axios';
import StorageSelector from './Selectors/StorageSelector.jsx';
import ErrorViewComponent from "./multistep/common/ErrorViewComponent.jsx";

class ServerDefaults extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            currentValues: {},
            storageList: [],
            loading: false,
            error: null
        };

        this.keys = {
            storage: "project_storage_id"
        };

        this.updateDefaultStorage = this.updateDefaultStorage.bind(this);

    }

    componentWillMount(){
        this.refreshData();
    }

    refreshData(){
        this.setState({loading:true}, ()=>{
            Promise.all([axios.get("/api/default"),axios.get("/api/storage")]).then(responses=>{
                this.setState({
                    loading: false,
                    error: null,
                    storageList: responses[1].data.result,
                    currentValues: responses[0].data.results.reduce((acc,entry)=>{
                        acc[entry.name]=entry.value;
                        return acc;
                    }, {})
                })
            }).catch(error=>this.setState({loading: false, error: error}))
        })
    }

    updateDefaultStorage(newStorageId){
        axios.put("/api/default/" + this.keys.storage, newStorageId, {headers: {'Content-Type': 'text/plain'}}).then(
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

    render(){
        return <div className="mainbody">
            <h3>Server defaults</h3>
            <ErrorViewComponent error={this.state.error}/>
            <table>
                <tbody>
                <tr>
                    <td>Default storage for created projects</td>
                    <td><StorageSelector selectedStorage={this.storagePref()}
                                         selectionUpdated={this.updateDefaultStorage}
                                         storageList={this.state.storageList}/></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default ServerDefaults;