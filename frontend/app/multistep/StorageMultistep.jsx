import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';

import StorageTypeComponent from './storage/TypeComponent.jsx';
import StorageLoginComponent from './storage/LoginComponent.jsx';
import StorageSubfolderComponent from './storage/SubfolderComponent.jsx';
import StorageCompletionComponent from './storage/CompletionComponent.jsx';

class StorageMultistep extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            strgTypes: [],
            selectedType: 0,
            rootpath: "",
            loginDetails: {}
        }
    }

    componentWillMount() {
        axios.get("/api/storage/knowntypes").then((response)=>{
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
                                                  valueWasSet={(loginDetails)=>this.setState({loginDetails: loginDetails})}/>
            },
            {
                name: 'Subfolder location',
                component: <StorageSubfolderComponent currentStorage={this.props.currentEntry}
                                                      strgTypes={this.state.strgTypes}
                                                      selectedType={this.state.selectedType}
                                                      valueWasSet={(value)=>this.setState({rootpath: value})}/>
            },
            {
                name: 'Confirm',
                component: <StorageCompletionComponent currentStorage={this.props.currentEntry}
                                                       strgTypes={this.state.strgTypes}
                                                       selectedType={this.state.selectedType}
                                                       loginDetails={this.state.loginDetails}
                                                       rootpath={this.state.rootpath}/>
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default StorageMultistep;
