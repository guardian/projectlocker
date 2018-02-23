import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

class ProjectTypeView extends React.Component {
    static propTypes = {
        projectType: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            loading: false,
            content: {}
        }
    }

    componentWillMount(){
        this.setState({loading: true}, ()=>{
            axios.get("/api/projecttype/" + this.props.projectType)
                .then(response=>this.setState({content: response.data.result, loading: false}))
                .catch(error=>this.setState({lastError: error, loading: false}))
        })
    }

    render(){
        return <span>{this.state.content.name}</span>
    }
}

export default ProjectTypeView;
