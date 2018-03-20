import React from 'react';
import PropTypes from 'prop-types';
import ProjectEntryView from './ProjectEntryView.jsx';
import ProjectTemplateEntryView from './ProjectTemplateEntryView.jsx';
import ErrorViewComponent from '../multistep/common/ErrorViewComponent.jsx';
import axios from 'axios';

class FileReferencesView extends React.Component {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            projectRefs: [],
            templateRefs: [],
            loading: false,
            error: null
        }
    }

    componentWillMount(){
        this.setState({loading: true}, ()=> {
            axios.get("/api/file/" + this.props.entryId + "/associations").then(response => {
                this.setState({loading: false, projectRefs: response.data.projects, templateRefs: response.data.templates})
            }).catch(error=>
                this.setState({loading: false, error: error})
            );
        });
    }

    render(){
        if(this.state.loading) return <img src="/assets/images/uploading.svg" style={{height: "20px"}}/>;

        if(this.state.error) return <ErrorViewComponent error={this.state.error}/>;

        if(this.state.projectRefs.length===0 && this.state.templateRefs.length===0)
            return <h4>Not referenced</h4>

        return <div><h4>Referenced by</h4><ul className="no-gap">
            {this.state.projectRefs.map(ref=><li><ProjectEntryView entryId={ref.id}
                                                         created={ref.created}
                                                         projectTypeId={ref.projectTypeId}
                                                         title={ref.title}
                                                         user={ref.user}/></li>)}
            {this.state.templateRefs.map(ref=><li><ProjectTemplateEntryView entryId={ref.id}
                                                                      projectTypeId={ref.projectTypeId}
                                                                      name={ref.name}/>

            </li>)}
        </ul></div>
    }
}

export default FileReferencesView;